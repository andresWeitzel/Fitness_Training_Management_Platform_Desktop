package com.fitnesstraining.members.repository;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.util.List;
import java.util.Optional;

public class ClientRepository {

    private final PersistenceManager persistence;

    public ClientRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public long countActive() {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT COUNT(c) FROM Client c
                                WHERE c.deletedAt IS NULL AND c.status = :status
                                """, Long.class)
                        .setParameter("status", ClientStatus.ACTIVE)
                        .getSingleResult());
    }

    public long countInactive() {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT COUNT(c) FROM Client c WHERE c.deletedAt IS NOT NULL", Long.class)
                        .getSingleResult());
    }

    public List<Client> findRecent(int limit) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT c FROM Client c
                                WHERE c.deletedAt IS NULL
                                ORDER BY c.createdAt DESC
                                """, Client.class)
                        .setMaxResults(limit)
                        .getResultList());
    }

    public long countAll() {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT COUNT(c) FROM Client c WHERE c.deletedAt IS NULL", Long.class)
                        .getSingleResult());
    }

    public Optional<Client> findById(Long id) {
        return persistence.inTransaction(em -> Optional.ofNullable(em.find(Client.class, id))
                .filter(client -> !client.isDeleted()));
    }

    public boolean existsDocument(String documentNumber, Long excludeId) {
        return persistence.inTransaction(em -> {
            String jpql = excludeId == null
                    ? """
                      SELECT COUNT(c) FROM Client c
                      WHERE lower(c.documentNumber) = lower(:document)
                        AND c.deletedAt IS NULL
                      """
                    : """
                      SELECT COUNT(c) FROM Client c
                      WHERE lower(c.documentNumber) = lower(:document)
                        AND c.deletedAt IS NULL
                        AND c.id <> :excludeId
                      """;
            var query = em.createQuery(jpql, Long.class)
                    .setParameter("document", documentNumber.trim());
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            return query.getSingleResult() > 0;
        });
    }

    public List<Client> search(String term) {
        String like = "%" + term.trim().toLowerCase() + "%";
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT c FROM Client c
                                WHERE c.deletedAt IS NULL
                                  AND (
                                    lower(c.documentNumber) LIKE :term
                                    OR lower(c.firstName) LIKE :term
                                    OR lower(c.lastName) LIKE :term
                                    OR lower(c.email) LIKE :term
                                  )
                                ORDER BY c.lastName, c.firstName
                                """, Client.class)
                        .setParameter("term", like)
                        .getResultList());
    }

    public List<Client> findAllActiveRecords() {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT c FROM Client c
                                WHERE c.deletedAt IS NULL
                                ORDER BY c.lastName, c.firstName
                                """, Client.class)
                        .getResultList());
    }

    public Client save(Client client) {
        return persistence.inTransaction(em -> {
            if (client.getId() == null) {
                em.persist(client);
                em.flush();
                return client;
            }
            return em.merge(client);
        });
    }
}
