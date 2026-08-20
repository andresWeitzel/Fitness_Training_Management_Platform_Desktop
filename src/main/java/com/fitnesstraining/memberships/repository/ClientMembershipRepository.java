package com.fitnesstraining.memberships.repository;

import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipListScope;
import com.fitnesstraining.memberships.model.MembershipStatus;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class ClientMembershipRepository {

    private final PersistenceManager persistence;

    public ClientMembershipRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<ClientMembership> findById(Long id) {
        return findByIdWithDetails(id);
    }

    public Optional<ClientMembership> findByIdWithDetails(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT m FROM ClientMembership m
                                JOIN FETCH m.client c
                                JOIN FETCH m.plan p
                                WHERE m.id = :id
                                """, ClientMembership.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public Optional<ClientMembership> findActiveByClientId(Long clientId) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT m FROM ClientMembership m
                                JOIN FETCH m.client c
                                JOIN FETCH m.plan p
                                WHERE m.client.id = :clientId
                                  AND m.status = :status
                                """, ClientMembership.class)
                        .setParameter("clientId", clientId)
                        .setParameter("status", MembershipStatus.ACTIVE)
                        .setMaxResults(1)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<ClientMembership> list(MembershipListScope scope, OffsetDateTime now) {
        return persistence.inTransaction(em -> {
            var query = em.createQuery("""
                            SELECT m FROM ClientMembership m
                            JOIN FETCH m.client c
                            JOIN FETCH m.plan p
                            WHERE %s
                            ORDER BY m.endsAt DESC, c.lastName, c.firstName
                            """.formatted(scopePredicate(scope)), ClientMembership.class);
            bindNowIfNeeded(query, scope, now);
            return query.getResultList();
        });
    }

    public List<ClientMembership> search(String term, MembershipListScope scope, OffsetDateTime now) {
        String like = "%" + term.trim().toLowerCase() + "%";
        return persistence.inTransaction(em -> {
            var query = em.createQuery("""
                            SELECT m FROM ClientMembership m
                            JOIN FETCH m.client c
                            JOIN FETCH m.plan p
                            WHERE (%s)
                              AND (
                                lower(c.documentNumber) LIKE :term
                                OR lower(c.firstName) LIKE :term
                                OR lower(c.lastName) LIKE :term
                                OR lower(p.name) LIKE :term
                              )
                            ORDER BY m.endsAt DESC, c.lastName, c.firstName
                            """.formatted(scopePredicate(scope)), ClientMembership.class)
                    .setParameter("term", like);
            bindNowIfNeeded(query, scope, now);
            return query.getResultList();
        });
    }

    public ClientMembership save(ClientMembership membership) {
        return persistence.inTransaction(em -> {
            if (membership.getId() == null) {
                em.persist(membership);
                em.flush();
                return membership;
            }
            return em.merge(membership);
        });
    }

    private static void bindNowIfNeeded(
            jakarta.persistence.TypedQuery<ClientMembership> query,
            MembershipListScope scope,
            OffsetDateTime now) {
        if (scope != MembershipListScope.ALL) {
            query.setParameter("now", now);
        }
    }

    private static String scopePredicate(MembershipListScope scope) {
        return switch (scope) {
            case ACTIVE -> "m.status = com.fitnesstraining.memberships.model.MembershipStatus.ACTIVE AND m.endsAt > :now";
            case EXPIRED -> """
                    (
                        (m.status = com.fitnesstraining.memberships.model.MembershipStatus.ACTIVE AND m.endsAt <= :now)
                        OR m.status = com.fitnesstraining.memberships.model.MembershipStatus.EXPIRED
                    )
                    """;
            case ALL -> "1 = 1";
        };
    }
}
