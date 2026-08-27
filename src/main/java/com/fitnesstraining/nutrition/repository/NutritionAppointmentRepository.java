package com.fitnesstraining.nutrition.repository;

import com.fitnesstraining.nutrition.model.NutritionAppointment;
import com.fitnesstraining.nutrition.model.NutritionAppointmentListScope;
import com.fitnesstraining.nutrition.model.NutritionAppointmentStatus;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class NutritionAppointmentRepository {

    private final PersistenceManager persistence;

    public NutritionAppointmentRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public Optional<NutritionAppointment> findById(Long id) {
        return persistence.inTransaction(em ->
                em.createQuery("""
                                SELECT a FROM NutritionAppointment a
                                JOIN FETCH a.client client
                                JOIN FETCH a.nutritionist nutritionist
                                WHERE a.id = :id
                                """, NutritionAppointment.class)
                        .setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst());
    }

    public List<NutritionAppointment> list(
            String term,
            NutritionAppointmentListScope scope,
            OffsetDateTime now,
            OffsetDateTime since) {
        StringBuilder jpql = new StringBuilder("""
                SELECT a FROM NutritionAppointment a
                JOIN FETCH a.client client
                JOIN FETCH a.nutritionist nutritionist
                WHERE 1 = 1
                """);
        if (scope == NutritionAppointmentListScope.UPCOMING) {
            jpql.append(" AND a.status = :scheduledStatus AND a.scheduledAt >= :now");
        } else if (scope == NutritionAppointmentListScope.LAST_30_DAYS && since != null) {
            jpql.append(" AND a.scheduledAt >= :since");
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
        jpql.append(" ORDER BY a.scheduledAt DESC, a.id DESC");

        return persistence.inTransaction(em -> {
            var query = em.createQuery(jpql.toString(), NutritionAppointment.class);
            if (scope == NutritionAppointmentListScope.UPCOMING) {
                query.setParameter("scheduledStatus", NutritionAppointmentStatus.SCHEDULED);
                query.setParameter("now", now);
            } else if (scope == NutritionAppointmentListScope.LAST_30_DAYS && since != null) {
                query.setParameter("since", since);
            }
            if (term != null && !term.isBlank()) {
                query.setParameter("term", "%" + term.trim().toLowerCase() + "%");
            }
            return query.getResultList();
        });
    }

    public void cancelScheduledForClient(Long clientId) {
        persistence.inTransaction(em -> {
            List<NutritionAppointment> appointments = em.createQuery("""
                            SELECT a FROM NutritionAppointment a
                            WHERE a.client.id = :clientId
                              AND a.status = :scheduled
                            """, NutritionAppointment.class)
                    .setParameter("clientId", clientId)
                    .setParameter("scheduled", NutritionAppointmentStatus.SCHEDULED)
                    .getResultList();
            for (NutritionAppointment appointment : appointments) {
                appointment.setStatus(NutritionAppointmentStatus.CANCELLED);
            }
            return null;
        });
    }

    public NutritionAppointment save(NutritionAppointment appointment) {
        return persistence.inTransaction(em -> {
            if (appointment.getId() == null) {
                em.persist(appointment);
                return appointment;
            }
            return em.merge(appointment);
        });
    }
}
