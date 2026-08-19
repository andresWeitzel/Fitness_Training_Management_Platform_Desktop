package com.fitnesstraining.members.repository;

import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.util.List;
import java.util.Optional;

public class AccessCredentialRepository {

    private final PersistenceManager persistence;

    public AccessCredentialRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public String nextCode(CredentialType type) {
        String sequence = switch (type) {
            case CLIENT_NUMBER -> "client_number_seq";
            case CARD -> "card_number_seq";
            case QR -> "qr_code_seq";
        };
        String prefix = switch (type) {
            case CLIENT_NUMBER -> "CLI-";
            case CARD -> "CARD-";
            case QR -> "QR-";
        };
        return persistence.inTransaction(em -> {
            Number value = (Number) em.createNativeQuery("SELECT nextval('" + sequence + "')")
                    .getSingleResult();
            return prefix + "%06d".formatted(value.longValue());
        });
    }

    public List<AccessCredential> findByClientId(Long clientId) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT a FROM AccessCredential a
                                WHERE a.client.id = :clientId
                                ORDER BY a.issuedAt DESC
                                """, AccessCredential.class)
                        .setParameter("clientId", clientId)
                        .getResultList());
    }

    public Optional<AccessCredential> findActiveByClientAndType(Long clientId, CredentialType type) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT a FROM AccessCredential a
                                WHERE a.client.id = :clientId
                                  AND a.type = :type
                                  AND a.active = TRUE
                                ORDER BY a.issuedAt DESC
                                """, AccessCredential.class)
                        .setParameter("clientId", clientId)
                        .setParameter("type", type)
                        .setMaxResults(1)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public Optional<String> findClientNumber(Long clientId) {
        return findActiveByClientAndType(clientId, CredentialType.CLIENT_NUMBER)
                .map(AccessCredential::getCode);
    }

    public AccessCredential addToClient(Long clientId, AccessCredential credential) {
        return persistence.inTransaction(em -> {
            Client client = em.find(Client.class, clientId);
            credential.assignTo(client);
            em.persist(credential);
            em.flush();
            return credential;
        });
    }

    public AccessCredential save(AccessCredential credential) {
        return persistence.inTransaction(em -> em.merge(credential));
    }

    public void deactivateAllForClient(Long clientId) {
        persistence.inTransaction(em ->
                em.createQuery("""
                                UPDATE AccessCredential a
                                SET a.active = false
                                WHERE a.client.id = :clientId AND a.active = true
                                """)
                        .setParameter("clientId", clientId)
                        .executeUpdate());
    }
}
