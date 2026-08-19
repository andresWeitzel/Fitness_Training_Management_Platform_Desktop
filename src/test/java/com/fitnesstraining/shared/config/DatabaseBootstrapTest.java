package com.fitnesstraining.shared.config;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBootstrapTest {

    @Test
    void explainsRefusedConnection() {
        var settings = new DatabaseSettings("localhost", 5432, "fitness_training", "postgres", "");
        String message = DatabaseBootstrap.humanize(
                new SQLException("Connection to localhost:5432 refused. Check that the hostname and port are correct"),
                settings
        );
        assertTrue(message.contains("no está en ejecución"));
        assertTrue(message.contains("docker compose up -d"));
    }
}
