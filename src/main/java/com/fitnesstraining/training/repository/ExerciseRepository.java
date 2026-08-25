package com.fitnesstraining.training.repository;

import com.fitnesstraining.shared.config.PersistenceManager;
import com.fitnesstraining.training.model.Exercise;

import java.util.List;
import java.util.Optional;

public class ExerciseRepository {

    private final PersistenceManager persistence;

    public ExerciseRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public long count() {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT COUNT(e) FROM Exercise e", Long.class).getSingleResult());
    }

    public List<Exercise> findAll() {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT e FROM Exercise e
                                ORDER BY e.active DESC, e.muscleGroup, e.name
                                """, Exercise.class)
                        .getResultList());
    }

    public List<Exercise> findActive() {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT e FROM Exercise e
                                WHERE e.active = TRUE
                                ORDER BY e.muscleGroup, e.name
                                """, Exercise.class)
                        .getResultList());
    }

    public List<Exercise> search(String term, Boolean activeOnly) {
        String like = "%" + term.trim().toLowerCase() + "%";
        return persistence.inTransaction(em -> {
            String activePredicate = activeOnly == null
                    ? "1 = 1"
                    : (activeOnly ? "e.active = TRUE" : "e.active = FALSE");
            return em.createQuery("""
                            SELECT e FROM Exercise e
                            WHERE (%s)
                              AND (
                                lower(e.name) LIKE :term
                                OR lower(coalesce(e.description, '')) LIKE :term
                              )
                            ORDER BY e.active DESC, e.name
                            """.formatted(activePredicate), Exercise.class)
                    .setParameter("term", like)
                    .getResultList();
        });
    }

    public Optional<Exercise> findById(Long id) {
        return persistence.inTransaction(em -> Optional.ofNullable(em.find(Exercise.class, id)));
    }

    public boolean existsName(String name, Long excludeId) {
        return persistence.inTransaction(em -> {
            String jpql = excludeId == null
                    ? """
                      SELECT COUNT(e) FROM Exercise e
                      WHERE lower(e.name) = lower(:name)
                      """
                    : """
                      SELECT COUNT(e) FROM Exercise e
                      WHERE lower(e.name) = lower(:name)
                        AND e.id <> :excludeId
                      """;
            var query = em.createQuery(jpql, Long.class).setParameter("name", name.trim());
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            return query.getSingleResult() > 0;
        });
    }

    public Exercise save(Exercise exercise) {
        return persistence.inTransaction(em -> {
            if (exercise.getId() == null) {
                em.persist(exercise);
                em.flush();
                return exercise;
            }
            return em.merge(exercise);
        });
    }
}
