package com.fitnesstraining.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class DatabaseConfigStore {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfigStore.class);
    private static final String FILE_NAME = "database.properties";

    private final Path file;

    public DatabaseConfigStore() {
        this(Path.of(System.getProperty("user.home"), ".fitness-training", FILE_NAME));
    }

    public DatabaseConfigStore(Path file) {
        this.file = file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    public Path location() {
        return file;
    }

    public Optional<DatabaseSettings> load() {
        if (!exists()) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
            return Optional.of(new DatabaseSettings(
                    properties.getProperty("db.host", "localhost"),
                    Integer.parseInt(properties.getProperty("db.port", "5432")),
                    properties.getProperty("db.name", "fitness_training"),
                    properties.getProperty("db.user", "postgres"),
                    properties.getProperty("db.password", "")
            ));
        } catch (Exception ex) {
            log.warn("No se pudo leer la configuración de PostgreSQL en {}", file, ex);
            return Optional.empty();
        }
    }

    public void save(DatabaseSettings settings) {
        try {
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            properties.setProperty("db.host", settings.host());
            properties.setProperty("db.port", String.valueOf(settings.port()));
            properties.setProperty("db.name", settings.database());
            properties.setProperty("db.user", settings.username());
            properties.setProperty("db.password", settings.password());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Fitness Training Management Platform — PostgreSQL");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la configuración de PostgreSQL.", ex);
        }
    }
}
