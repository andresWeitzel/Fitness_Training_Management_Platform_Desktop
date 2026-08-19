package com.fitnesstraining.shared.config;

import java.util.Objects;

public record DatabaseSettings(
        String host,
        int port,
        String database,
        String username,
        String password
) {

    public DatabaseSettings {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        if (host.isBlank() || database.isBlank() || username.isBlank()) {
            throw new IllegalArgumentException("Host, base de datos y usuario son obligatorios.");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Puerto inválido.");
        }
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(host.trim(), port, database.trim());
    }

    public String jdbcServerUrl() {
        return "jdbc:postgresql://%s:%d/postgres".formatted(host.trim(), port);
    }
}
