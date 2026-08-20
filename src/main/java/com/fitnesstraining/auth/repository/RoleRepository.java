package com.fitnesstraining.auth.repository;

import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.shared.config.PersistenceManager;

import java.util.List;
import java.util.Optional;

public class RoleRepository {

    private final PersistenceManager persistence;

    public RoleRepository(PersistenceManager persistence) {
        this.persistence = persistence;
    }

    public List<Role> findAll() {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT r FROM Role r ORDER BY r.name", Role.class)
                        .getResultList());
    }

    public Optional<Role> findByName(String name) {
        return persistence.inTransaction(em ->
                em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                        .setParameter("name", name)
                        .getResultList()
                        .stream()
                        .findFirst());
    }
}
