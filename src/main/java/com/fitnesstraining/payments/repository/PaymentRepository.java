package com.fitnesstraining.payments.repository;

import com.fitnesstraining.payments.model.Payment;
import com.fitnesstraining.payments.model.PaymentListScope;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class PaymentRepository {

    private final PersistenceManager persistence;

    public PaymentRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<Payment> findById(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT p FROM Payment p
                                JOIN FETCH p.client c
                                LEFT JOIN FETCH p.clientMembership m
                                LEFT JOIN FETCH m.plan
                                WHERE p.id = :id
                                """, Payment.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<Payment> list(PaymentListScope scope, OffsetDateTime now) {
        return persistence.inTransaction(em -> {
            var query = em.createQuery("""
                            SELECT p FROM Payment p
                            JOIN FETCH p.client c
                            LEFT JOIN FETCH p.clientMembership m
                            LEFT JOIN FETCH m.plan
                            WHERE %s
                            ORDER BY COALESCE(p.paidAt, p.dueAt, p.createdAt) DESC, c.lastName, c.firstName
                            """.formatted(scopePredicate(scope)), Payment.class);
            bindNowIfNeeded(query, scope, now);
            return query.getResultList();
        });
    }

    public List<Payment> search(String term, PaymentListScope scope, OffsetDateTime now) {
        String like = "%" + term.trim().toLowerCase() + "%";
        return persistence.inTransaction(em -> {
            var query = em.createQuery("""
                            SELECT p FROM Payment p
                            JOIN FETCH p.client c
                            LEFT JOIN FETCH p.clientMembership m
                            LEFT JOIN FETCH m.plan
                            WHERE (%s)
                              AND (
                                lower(c.documentNumber) LIKE :term
                                OR lower(c.firstName) LIKE :term
                                OR lower(c.lastName) LIKE :term
                              )
                            ORDER BY COALESCE(p.paidAt, p.dueAt, p.createdAt) DESC, c.lastName, c.firstName
                            """.formatted(scopePredicate(scope)), Payment.class)
                    .setParameter("term", like);
            bindNowIfNeeded(query, scope, now);
            return query.getResultList();
        });
    }

    public boolean hasOpenDebt(Long clientId) {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(p) FROM Payment p
                                WHERE p.client.id = :clientId
                                  AND p.status = :pending
                                """, Long.class)
                        .setParameter("clientId", clientId)
                        .setParameter("pending", PaymentStatus.PENDING)
                        .getSingleResult());
        return count != null && count > 0;
    }

    public boolean hasPaidDailyPassOnDay(Long clientId, OffsetDateTime dayStart, OffsetDateTime dayEnd) {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(p) FROM Payment p
                                WHERE p.client.id = :clientId
                                  AND p.status = :paid
                                  AND p.type = :dailyPass
                                  AND p.paidAt IS NOT NULL
                                  AND p.paidAt >= :dayStart
                                  AND p.paidAt < :dayEnd
                                """, Long.class)
                        .setParameter("clientId", clientId)
                        .setParameter("paid", PaymentStatus.PAID)
                        .setParameter("dailyPass", com.fitnesstraining.payments.model.PaymentType.DAILY_PASS)
                        .setParameter("dayStart", dayStart)
                        .setParameter("dayEnd", dayEnd)
                        .getSingleResult());
        return count != null && count > 0;
    }

    public long countByClientId(Long clientId) {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(p) FROM Payment p
                                WHERE p.client.id = :clientId
                                """, Long.class)
                        .setParameter("clientId", clientId)
                        .getSingleResult());
        return count == null ? 0L : count;
    }

    public Payment save(Payment payment) {
        return persistence.inTransaction(em -> {
            if (payment.getId() == null) {
                em.persist(payment);
                em.flush();
                return payment;
            }
            return em.merge(payment);
        });
    }

    private static void bindNowIfNeeded(
            jakarta.persistence.TypedQuery<Payment> query,
            PaymentListScope scope,
            OffsetDateTime now) {
        if (scope == PaymentListScope.OVERDUE || scope == PaymentListScope.PENDING) {
            query.setParameter("now", now);
        }
    }

    private static String scopePredicate(PaymentListScope scope) {
        return switch (scope) {
            case PENDING -> """
                    p.status = com.fitnesstraining.payments.model.PaymentStatus.PENDING
                    AND (p.dueAt IS NULL OR p.dueAt > :now)
                    """;
            case OVERDUE -> """
                    p.status = com.fitnesstraining.payments.model.PaymentStatus.PENDING
                    AND p.dueAt IS NOT NULL
                    AND p.dueAt <= :now
                    """;
            case PAID -> "p.status = com.fitnesstraining.payments.model.PaymentStatus.PAID";
            case CANCELLED -> "p.status = com.fitnesstraining.payments.model.PaymentStatus.CANCELLED";
            case ALL -> "1 = 1";
        };
    }
}
