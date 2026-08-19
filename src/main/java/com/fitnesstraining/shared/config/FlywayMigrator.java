package com.fitnesstraining.shared.config;

import com.fitnesstraining.shared.exception.AppException;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlywayMigrator {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrator.class);

    public void migrate(DatabaseSettings settings) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(settings.jdbcUrl(), settings.username(), settings.password())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            var result = flyway.migrate();
            log.info("Flyway aplicó {} migración(es).", result.migrationsExecuted);
        } catch (Exception ex) {
            throw new AppException("Falló la migración de la base de datos: " + ex.getMessage(), ex);
        }
    }
}
