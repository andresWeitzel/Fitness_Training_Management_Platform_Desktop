package com.fitnesstraining.training.repository;

import com.fitnesstraining.shared.config.PersistenceManager;
import com.fitnesstraining.training.model.RoutineStatus;
import com.fitnesstraining.training.model.TrainingRoutine;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class TrainingRoutineRepository {

    private final PersistenceManager persistence;

    public TrainingRoutineRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public long count() {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT COUNT(r) FROM TrainingRoutine r", Long.class).getSingleResult());
    }

    public List<TrainingRoutine> list(RoutineStatus statusOrNull) {
        return persistence.inTransaction(em -> {
            if (statusOrNull == null) {
                return em.createQuery("""
                                SELECT DISTINCT r FROM TrainingRoutine r
                                LEFT JOIN FETCH r.items
                                ORDER BY r.updatedAt DESC
                                """, TrainingRoutine.class)
                        .getResultList();
            }
            return em.createQuery("""
                            SELECT DISTINCT r FROM TrainingRoutine r
                            LEFT JOIN FETCH r.items
                            WHERE r.status = :status
                            ORDER BY r.updatedAt DESC
                            """, TrainingRoutine.class)
                    .setParameter("status", statusOrNull)
                    .getResultList();
        });
    }

    public List<TrainingRoutine> search(String term, RoutineStatus statusOrNull) {
        String like = "%" + term.trim().toLowerCase() + "%";
        return persistence.inTransaction(em -> {
            String statusPredicate = statusOrNull == null ? "1 = 1" : "r.status = :status";
            var query = em.createQuery("""
                            SELECT DISTINCT r FROM TrainingRoutine r
                            LEFT JOIN FETCH r.items
                            WHERE (%s)
                              AND (
                                lower(r.title) LIKE :term
                                OR lower(coalesce(r.notes, '')) LIKE :term
                              )
                            ORDER BY r.updatedAt DESC
                            """.formatted(statusPredicate), TrainingRoutine.class)
                    .setParameter("term", like);
            if (statusOrNull != null) {
                query.setParameter("status", statusOrNull);
            }
            return query.getResultList();
        });
    }

    public Optional<TrainingRoutine> findById(Long id) {
        return persistence.inTransaction(em -> {
            List<TrainingRoutine> rows = em.createQuery("""
                            SELECT DISTINCT r FROM TrainingRoutine r
                            LEFT JOIN FETCH r.items
                            WHERE r.id = :id
                            """, TrainingRoutine.class)
                    .setParameter("id", id)
                    .getResultList();
            return rows.stream().findFirst();
        });
    }

    public Optional<TrainingRoutine> findCurrentByClientId(Long clientId) {
        return persistence.inTransaction(em -> {
            List<TrainingRoutine> active = em.createQuery("""
                            SELECT r FROM TrainingRoutine r
                            WHERE r.clientId = :clientId
                              AND r.status = :active
                            ORDER BY r.updatedAt DESC
                            """, TrainingRoutine.class)
                    .setParameter("clientId", clientId)
                    .setParameter("active", RoutineStatus.ACTIVE)
                    .setMaxResults(1)
                    .getResultList();
            if (!active.isEmpty()) {
                return Optional.of(active.get(0));
            }
            List<TrainingRoutine> scheduled = em.createQuery("""
                            SELECT r FROM TrainingRoutine r
                            WHERE r.clientId = :clientId
                              AND r.status = :scheduled
                            ORDER BY r.updatedAt DESC
                            """, TrainingRoutine.class)
                    .setParameter("clientId", clientId)
                    .setParameter("scheduled", RoutineStatus.SCHEDULED)
                    .setMaxResults(1)
                    .getResultList();
            return scheduled.stream().findFirst();
        });
    }

    public void archiveActiveAndScheduledForClient(Long clientId, OffsetDateTime now) {
        persistence.inTransaction(em -> {
            List<TrainingRoutine> routines = em.createQuery("""
                            SELECT r FROM TrainingRoutine r
                            WHERE r.clientId = :clientId
                              AND r.status IN :statuses
                            """, TrainingRoutine.class)
                    .setParameter("clientId", clientId)
                    .setParameter("statuses", List.of(RoutineStatus.ACTIVE, RoutineStatus.SCHEDULED))
                    .getResultList();
            for (TrainingRoutine routine : routines) {
                routine.changeStatus(RoutineStatus.ARCHIVED, now);
            }
            return null;
        });
    }

    public TrainingRoutine save(TrainingRoutine routine) {
        return persistence.inTransaction(em -> {
            if (routine.getId() == null) {
                em.persist(routine);
                em.flush();
                return routine;
            }
            return em.merge(routine);
        });
    }
}
