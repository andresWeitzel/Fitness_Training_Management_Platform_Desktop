package com.fitnesstraining.checkin.repository;

import com.fitnesstraining.checkin.model.CheckIn;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class CheckInRepository {

    private final PersistenceManager persistence;

    public CheckInRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<CheckIn> findById(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT c FROM CheckIn c
                                JOIN FETCH c.client client
                                LEFT JOIN FETCH c.credential
                                WHERE c.id = :id
                                """, CheckIn.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<CheckIn> listBetween(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT c FROM CheckIn c
                                JOIN FETCH c.client client
                                LEFT JOIN FETCH c.credential
                                WHERE c.checkedInAt >= :fromInclusive
                                  AND c.checkedInAt < :toExclusive
                                ORDER BY c.checkedInAt DESC
                                """, CheckIn.class)
                        .setParameter("fromInclusive", fromInclusive)
                        .setParameter("toExclusive", toExclusive)
                        .getResultList());
    }

    public long countBetween(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(c) FROM CheckIn c
                                WHERE c.checkedInAt >= :fromInclusive
                                  AND c.checkedInAt < :toExclusive
                                """, Long.class)
                        .setParameter("fromInclusive", fromInclusive)
                        .setParameter("toExclusive", toExclusive)
                        .getSingleResult());
        return count == null ? 0L : count;
    }

    public long countDistinctClientsBetween(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(DISTINCT c.client.id) FROM CheckIn c
                                WHERE c.checkedInAt >= :fromInclusive
                                  AND c.checkedInAt < :toExclusive
                                """, Long.class)
                        .setParameter("fromInclusive", fromInclusive)
                        .setParameter("toExclusive", toExclusive)
                        .getSingleResult());
        return count == null ? 0L : count;
    }

    public boolean hasCheckInToday(Long clientId, OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        Long count = persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(c) FROM CheckIn c
                                WHERE c.client.id = :clientId
                                  AND c.checkedInAt >= :fromInclusive
                                  AND c.checkedInAt < :toExclusive
                                """, Long.class)
                        .setParameter("clientId", clientId)
                        .setParameter("fromInclusive", fromInclusive)
                        .setParameter("toExclusive", toExclusive)
                        .getSingleResult());
        return count != null && count > 0;
    }

    public CheckIn save(CheckIn checkIn) {
        return persistence.inTransaction(em -> {
            if (checkIn.getId() == null) {
                em.persist(checkIn);
                em.flush();
                return checkIn;
            }
            return em.merge(checkIn);
        });
    }
}
