package com.fitnesstraining.auth.repository;

import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
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
            return null;
        });
    }
}
