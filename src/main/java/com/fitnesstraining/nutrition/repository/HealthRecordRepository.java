package com.fitnesstraining.nutrition.repository;

import com.fitnesstraining.nutrition.model.HealthRecordEntry;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.util.List;
import java.util.Optional;

public class HealthRecordRepository {

    private final PersistenceManager persistence;

    public HealthRecordRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<HealthRecordEntry> findById(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT e FROM HealthRecordEntry e
                                JOIN FETCH e.client client
                                JOIN FETCH e.recordedBy author
                                WHERE e.id = :id
                                """, HealthRecordEntry.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<HealthRecordEntry> listByClient(Long clientId) {
        return listByClient(clientId, null);
    }

    public List<HealthRecordEntry> listByClient(Long clientId, String term) {
        StringBuilder jpql = new StringBuilder("""
                SELECT e FROM HealthRecordEntry e
                JOIN FETCH e.client client
                JOIN FETCH e.recordedBy author
                WHERE client.id = :clientId
                """);
        if (term != null && !term.isBlank()) {
            jpql.append("""
                     AND (
                        lower(coalesce(e.allergies, '')) LIKE :term
                        OR lower(coalesce(e.restrictions, '')) LIKE :term
                        OR lower(coalesce(e.conditions, '')) LIKE :term
                        OR lower(coalesce(e.medications, '')) LIKE :term
                        OR lower(coalesce(e.notes, '')) LIKE :term
                        OR lower(author.displayName) LIKE :term
                     )
                    """);
        }
        jpql.append(" ORDER BY e.recordedAt DESC, e.id DESC");

        return persistence.inTransaction(em -> {
            var query = em.createQuery(jpql.toString(), HealthRecordEntry.class)
                    .setParameter("clientId", clientId);
            if (term != null && !term.isBlank()) {
                query.setParameter("term", "%" + term.trim().toLowerCase() + "%");
            }
            return query.getResultList();
        });
    }

    public List<HealthRecordEntry> listAll(String term) {
        StringBuilder jpql = new StringBuilder("""
                SELECT e FROM HealthRecordEntry e
                JOIN FETCH e.client client
                JOIN FETCH e.recordedBy author
                WHERE 1 = 1
                """);
        if (term != null && !term.isBlank()) {
            jpql.append("""
                     AND (
                        lower(client.documentNumber) LIKE :term
                        OR lower(client.firstName) LIKE :term
                        OR lower(client.lastName) LIKE :term
                        OR lower(coalesce(e.allergies, '')) LIKE :term
                        OR lower(coalesce(e.restrictions, '')) LIKE :term
                        OR lower(coalesce(e.conditions, '')) LIKE :term
                        OR lower(coalesce(e.medications, '')) LIKE :term
                        OR lower(coalesce(e.notes, '')) LIKE :term
                        OR lower(author.displayName) LIKE :term
                     )
                    """);
        }
        jpql.append(" ORDER BY e.recordedAt DESC, e.id DESC");

        return persistence.inTransaction(em -> {
            var query = em.createQuery(jpql.toString(), HealthRecordEntry.class);
            if (term != null && !term.isBlank()) {
                query.setParameter("term", "%" + term.trim().toLowerCase() + "%");
            }
            return query.getResultList();
        });
    }

    public HealthRecordEntry save(HealthRecordEntry entry) {
        return persistence.inTransaction(em -> {
            em.persist(entry);
            return entry;
        });
    }
}
