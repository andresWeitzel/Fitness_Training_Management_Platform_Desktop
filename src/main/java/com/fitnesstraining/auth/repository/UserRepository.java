package com.fitnesstraining.auth.repository;

import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final PersistenceManager persistence;

    public UserRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public long count() {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL", Long.class)
                        .getSingleResult());
    }

    public long countActiveAdmins() {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(DISTINCT u) FROM User u
                                JOIN u.roles r
                                WHERE u.deletedAt IS NULL
                                  AND u.active = TRUE
                                  AND r.name = 'ADMIN'
                                """, Long.class)
                        .getSingleResult());
        return count == null ? 0L : count;
    }

    public Optional<User> findById(Long id) {
        return persistence.inTransaction(em -> {
            var users = em.createQuery("""
                            SELECT u FROM User u
                            WHERE u.id = :id
                              AND u.deletedAt IS NULL
                            """, User.class)
                    .setParameter("id", id)
                    .getResultList();
            User user = users.stream().findFirst().orElse(null);
            if (user == null) {
                return Optional.empty();
            }
            user.getRoles().size();
            return Optional.of(user);
        });
    }

    public Optional<User> findActiveByUsername(String username) {
        return persistence.inTransaction(em -> {
            var users = em.createQuery("""
                            SELECT u FROM User u
                            WHERE lower(u.username) = lower(:username)
                              AND u.active = TRUE
                              AND u.deletedAt IS NULL
                            """, User.class)
                    .setParameter("username", username)
                    .getResultList();
            User user = users.stream().findFirst().orElse(null);
            if (user == null) {
                return Optional.empty();
            }
            user.getRoles().size();
            user.getRoles().forEach(role -> role.getPermissions().size());
            return Optional.of(user);
        });
    }

    public boolean existsUsername(String username, Long excludeId) {
        return persistence.inTransaction(em -> {
            String jpql = excludeId == null
                    ? """
                      SELECT COUNT(u) FROM User u
                      WHERE lower(u.username) = lower(:username)
                        AND u.deletedAt IS NULL
                      """
                    : """
                      SELECT COUNT(u) FROM User u
                      WHERE lower(u.username) = lower(:username)
                        AND u.deletedAt IS NULL
                        AND u.id <> :excludeId
                      """;
            var query = em.createQuery(jpql, Long.class)
                    .setParameter("username", username.trim());
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            return query.getSingleResult() > 0;
        });
    }

    public List<User> listAll() {
        return list(null);
    }

    public List<User> listActive() {
        return list(true);
    }

    public List<User> listInactive() {
        return list(false);
    }

    public List<User> search(String term, Boolean activeOnly) {
        String like = "%" + term.trim().toLowerCase() + "%";
        return persistence.inTransaction(em -> {
            String activePredicate = activeOnly == null
                    ? "1 = 1"
                    : (activeOnly ? "u.active = TRUE" : "u.active = FALSE");
            return em.createQuery("""
                            SELECT DISTINCT u FROM User u
                            LEFT JOIN FETCH u.roles r
                            WHERE u.deletedAt IS NULL
                              AND (%s)
                              AND (
                                lower(u.username) LIKE :term
                                OR lower(u.displayName) LIKE :term
                                OR lower(coalesce(u.email, '')) LIKE :term
                                OR lower(coalesce(r.name, '')) LIKE :term
                              )
                            ORDER BY u.displayName
                            """.formatted(activePredicate), User.class)
                    .setParameter("term", like)
                    .getResultList();
        });
    }

    public void updateLastLogin(Long userId, OffsetDateTime at) {
        persistence.inTransaction(em -> {
            User user = em.find(User.class, userId);
            if (user != null) {
                user.markLoggedIn(at);
            }
            return null;
        });
    }

    public void createWithRole(User user, String roleName) {
        persistence.inTransaction(em -> {
            Role role = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", roleName)
                    .getSingleResult();
            user.addRole(role);
            em.persist(user);
            em.flush();
            return null;
        });
    }

    public User updateStaff(
            Long userId,
            String username,
            String displayName,
            String email,
            String passwordHashOrNull,
            String roleName,
            OffsetDateTime now) {
        return persistence.inTransaction(em -> {
            User user = em.find(User.class, userId);
            if (user == null || user.getDeletedAt() != null) {
                throw new IllegalStateException("Usuario no encontrado.");
            }
            user.changeUsername(username, now);
            user.updateProfile(displayName, email, now);
            if (passwordHashOrNull != null) {
                user.changePassword(passwordHashOrNull, now);
            }
            Role role = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", roleName)
                    .getSingleResult();
            user.replaceRoles(java.util.Set.of(role), now);
            return user;
        });
    }

    public User save(User user) {
        return persistence.inTransaction(em -> {
            if (user.getId() == null) {
                em.persist(user);
                em.flush();
                return user;
            }
            return em.merge(user);
        });
    }

    private List<User> list(Boolean activeOnly) {
        return persistence.inTransaction(em -> {
            String activePredicate = activeOnly == null
                    ? "1 = 1"
                    : (activeOnly ? "u.active = TRUE" : "u.active = FALSE");
            return em.createQuery("""
                            SELECT DISTINCT u FROM User u
                            LEFT JOIN FETCH u.roles
                            WHERE u.deletedAt IS NULL
                              AND (%s)
                            ORDER BY u.displayName
                            """.formatted(activePredicate), User.class)
                    .getResultList();
        });
    }
}
