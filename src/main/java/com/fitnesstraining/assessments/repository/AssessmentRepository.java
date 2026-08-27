package com.fitnesstraining.assessments.repository;

import com.fitnesstraining.assessments.model.AssessmentListScope;
import com.fitnesstraining.assessments.model.PhysicalAssessment;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class AssessmentRepository {

    private final PersistenceManager persistence;

    public AssessmentRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<PhysicalAssessment> findById(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT a FROM PhysicalAssessment a
                                JOIN FETCH a.client client
                                JOIN FETCH a.assessedBy assessor
                                WHERE a.id = :id
                                """, PhysicalAssessment.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<PhysicalAssessment> list(String term, AssessmentListScope scope, OffsetDateTime since, Long clientId) {
        StringBuilder jpql = new StringBuilder("""
                SELECT a FROM PhysicalAssessment a
                JOIN FETCH a.client client
                JOIN FETCH a.assessedBy assessor
                WHERE 1 = 1
                """);
        if (since != null) {
            jpql.append(" AND a.assessedAt >= :since");
        }
        if (clientId != null) {
            jpql.append(" AND client.id = :clientId");
        }
        if (term != null && !term.isBlank()) {
            jpql.append("""
                     AND (
                        lower(client.documentNumber) LIKE :term
                        OR lower(client.firstName) LIKE :term
                        OR lower(client.lastName) LIKE :term
                        OR EXISTS (
                            SELECT 1 FROM AccessCredential ac
                            WHERE ac.client = client
                              AND lower(ac.code) LIKE :term
                        )
                     )
                    """);
        }
        jpql.append(" ORDER BY a.assessedAt DESC, a.id DESC");

        return persistence.inTransaction(em -> {
            var query = em.createQuery(jpql.toString(), PhysicalAssessment.class);
            if (since != null) {
                query.setParameter("since", since);
            }
            if (clientId != null) {
                query.setParameter("clientId", clientId);
            }
            if (term != null && !term.isBlank()) {
                query.setParameter("term", "%" + term.trim().toLowerCase() + "%");
            }
            return query.getResultList();
        });
    }

    public PhysicalAssessment save(PhysicalAssessment assessment) {
        return persistence.inTransaction(em -> {
            if (assessment.getId() == null) {
                em.persist(assessment);
                return assessment;
            }
            return em.merge(assessment);
        });
    }

    public Optional<PhysicalAssessment> findLatestByClientId(Long clientId) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT a FROM PhysicalAssessment a
                                WHERE a.client.id = :clientId
                                ORDER BY a.assessedAt DESC, a.id DESC
                                """, PhysicalAssessment.class)
                        .setParameter("clientId", clientId)
                        .setMaxResults(1)
                        .getResultList()
                        .stream()
                        .findFirst());
    }
}
