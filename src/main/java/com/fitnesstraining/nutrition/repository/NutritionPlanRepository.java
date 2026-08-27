package com.fitnesstraining.nutrition.repository;

import com.fitnesstraining.nutrition.model.NutritionPlan;
import com.fitnesstraining.nutrition.model.NutritionPlanListScope;
import com.fitnesstraining.nutrition.model.NutritionPlanStatus;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.util.List;
import java.util.Optional;

public class NutritionPlanRepository {

    private final PersistenceManager persistence;

    public NutritionPlanRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<NutritionPlan> findById(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT p FROM NutritionPlan p
                                JOIN FETCH p.client client
                                JOIN FETCH p.createdBy author
                                WHERE p.id = :id
                                """, NutritionPlan.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<NutritionPlan> list(String term, NutritionPlanListScope scope) {
        StringBuilder jpql = new StringBuilder("""
                SELECT p FROM NutritionPlan p
                JOIN FETCH p.client client
                JOIN FETCH p.createdBy author
                WHERE 1 = 1
                """);
        if (scope == NutritionPlanListScope.ACTIVE) {
            jpql.append(" AND p.status = :activeStatus");
        } else if (scope == NutritionPlanListScope.DRAFT) {
            jpql.append(" AND p.status = :draftStatus");
        } else if (scope == NutritionPlanListScope.ARCHIVED) {
            jpql.append(" AND p.status = :archivedStatus");
        }
        if (term != null && !term.isBlank()) {
            jpql.append("""
                     AND (
                        lower(client.documentNumber) LIKE :term
                        OR lower(client.firstName) LIKE :term
                        OR lower(client.lastName) LIKE :term
                        OR lower(p.title) LIKE :term
                        OR EXISTS (
                            SELECT 1 FROM AccessCredential ac
                            WHERE ac.client = client
                              AND lower(ac.code) LIKE :term
                        )
                     )
                    """);
        }
        jpql.append(" ORDER BY p.updatedAt DESC, p.id DESC");

        return persistence.inTransaction(em -> {
            var query = em.createQuery(jpql.toString(), NutritionPlan.class);
            if (scope == NutritionPlanListScope.ACTIVE) {
                query.setParameter("activeStatus", NutritionPlanStatus.ACTIVE);
            } else if (scope == NutritionPlanListScope.DRAFT) {
                query.setParameter("draftStatus", NutritionPlanStatus.DRAFT);
            } else if (scope == NutritionPlanListScope.ARCHIVED) {
                query.setParameter("archivedStatus", NutritionPlanStatus.ARCHIVED);
            }
            if (term != null && !term.isBlank()) {
                query.setParameter("term", "%" + term.trim().toLowerCase() + "%");
            }
            return query.getResultList();
        });
    }

    public void archiveOpenForClient(Long clientId) {
        persistence.inTransaction(em -> {
            List<NutritionPlan> plans = em.createQuery("""
                            SELECT p FROM NutritionPlan p
                            WHERE p.client.id = :clientId
                              AND p.status <> :archived
                            """, NutritionPlan.class)
                    .setParameter("clientId", clientId)
                    .setParameter("archived", NutritionPlanStatus.ARCHIVED)
                    .getResultList();
            for (NutritionPlan plan : plans) {
                plan.setStatus(NutritionPlanStatus.ARCHIVED);
            }
            return null;
        });
    }

    public NutritionPlan save(NutritionPlan plan) {
        return persistence.inTransaction(em -> {
            if (plan.getId() == null) {
                em.persist(plan);
                return plan;
            }
            return em.merge(plan);
        });
    }

    public Optional<NutritionPlan> findActiveByClientId(Long clientId) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT p FROM NutritionPlan p
                                WHERE p.client.id = :clientId
                                  AND p.status = :active
                                ORDER BY p.updatedAt DESC, p.id DESC
                                """, NutritionPlan.class)
                        .setParameter("clientId", clientId)
                        .setParameter("active", NutritionPlanStatus.ACTIVE)
                        .setMaxResults(1)
                        .getResultList()
                        .stream()
                        .findFirst());
    }
}
