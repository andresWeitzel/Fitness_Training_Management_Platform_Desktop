package com.fitnesstraining.shared.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {

    private final Properties values = new Properties();

    public static AppProperties loadClasspath() {
        AppProperties properties = new AppProperties();
        try (InputStream in = AppProperties.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.values.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer application.properties.", ex);
        }
        return properties;
    }

    public String get(String key, String fallback) {
        String value = values.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public DatabaseSettings toDatabaseSettings() {
        return toDatabaseSettings(get("db.password", "postgres"));
    }

    public DatabaseSettings toDatabaseSettings(String password) {
        return new DatabaseSettings(
                get("db.host", "localhost"),
                getInt("db.port", 5432),
                get("db.name", "fitness_training"),
                get("db.user", "postgres"),
                password == null ? "" : password
        );
    }
}
