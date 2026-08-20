package com.fitnesstraining.memberships.repository;

import com.fitnesstraining.memberships.model.MembershipPlan;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.util.List;
import java.util.Optional;

public class MembershipPlanRepository {

    private final PersistenceManager persistence;

    public MembershipPlanRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public List<MembershipPlan> findAll() {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT p FROM MembershipPlan p
                                ORDER BY p.active DESC, p.name
                                """, MembershipPlan.class)
                        .getResultList());
    }

    public List<MembershipPlan> findActive() {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT p FROM MembershipPlan p
                                WHERE p.active = TRUE
                                ORDER BY p.name
                                """, MembershipPlan.class)
                        .getResultList());
    }

    public Optional<MembershipPlan> findById(Long id) {
        return persistence.inTransaction(em -> Optional.ofNullable(em.find(MembershipPlan.class, id)));
    }

    public boolean existsName(String name, Long excludeId) {
        return persistence.inTransaction(em -> {
            String jpql = excludeId == null
                    ? """
                      SELECT COUNT(p) FROM MembershipPlan p
                      WHERE lower(p.name) = lower(:name)
                      """
                    : """
                      SELECT COUNT(p) FROM MembershipPlan p
                      WHERE lower(p.name) = lower(:name)
                        AND p.id <> :excludeId
                      """;
            var query = em.createQuery(jpql, Long.class)
                    .setParameter("name", name.trim());
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            return query.getSingleResult() > 0;
        });
    }

    public MembershipPlan save(MembershipPlan plan) {
        return persistence.inTransaction(em -> {
            if (plan.getId() == null) {
                em.persist(plan);
                em.flush();
                return plan;
            }
            return em.merge(plan);
        });
    }
}
