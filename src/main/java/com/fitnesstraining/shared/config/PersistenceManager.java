package com.fitnesstraining.shared.config;

import com.fitnesstraining.shared.exception.AppException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PersistenceManager implements AutoCloseable {

    private final EntityManagerFactory entityManagerFactory;

    public PersistenceManager(DatabaseSettings settings, boolean showSql) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("jakarta.persistence.jdbc.url", settings.jdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", settings.username());
        properties.put("jakarta.persistence.jdbc.password", settings.password());
        properties.put("hibernate.show_sql", String.valueOf(showSql));
        properties.put("hibernate.format_sql", "true");
        this.entityManagerFactory = Persistence.createEntityManagerFactory("fitness-pu", properties);
    }

    public <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = work.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw ex instanceof AppException ? ex : new AppException(ex.getMessage(), ex);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
}
