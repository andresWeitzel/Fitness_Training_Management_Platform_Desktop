package com.fitnesstraining.shared.config;

import com.fitnesstraining.shared.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrap.class);

    public void testConnection(DatabaseSettings settings) {
        try (Connection connection = DriverManager.getConnection(
                settings.jdbcServerUrl(), settings.username(), settings.password())) {
            if (!connection.isValid(5)) {
                throw new AppException("La conexión a PostgreSQL no es válida.");
            }
        } catch (SQLException ex) {
            throw new AppException(humanize(ex, settings), ex);
        }
    }

    public void ensureDatabaseExists(DatabaseSettings settings) {
        String sql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (Connection connection = DriverManager.getConnection(
                settings.jdbcServerUrl(), settings.username(), settings.password());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, settings.database());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
            try (Statement create = connection.createStatement()) {
                create.execute("CREATE DATABASE " + quoteIdentifier(settings.database()));
                log.info("Base de datos {} creada.", settings.database());
            }
        } catch (SQLException ex) {
            throw new AppException(humanize(ex, settings), ex);
        }
    }

    static String humanize(SQLException ex, DatabaseSettings settings) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        String lower = raw.toLowerCase();
        if (lower.contains("refused") || lower.contains("the connection attempt failed") || lower.contains("connection refused")) {
            return """
                    PostgreSQL no está en ejecución en %s:%d.

                    Opción recomendada (Docker), desde la carpeta del proyecto:
                      docker compose up -d

                    Datos de conexión Docker:
                      usuario postgres · contraseña postgres · base fitness_training

                    Después volvé a probar la conexión.
                    """.formatted(settings.host(), settings.port());
        }
        if (raw.contains("password authentication failed") || raw.contains("28P01")) {
            return "Usuario o contraseña de PostgreSQL incorrectos.";
        }
        return "No se pudo conectar a PostgreSQL: " + raw;
    }

    private static String quoteIdentifier(String name) {
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new AppException("Nombre de base de datos inválido: " + name);
        }
        return name;
    }
}
