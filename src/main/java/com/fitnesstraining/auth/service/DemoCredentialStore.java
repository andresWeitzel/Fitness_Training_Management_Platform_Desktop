package com.fitnesstraining.auth.service;

import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Credenciales en claro para el panel de cuentas de prueba.
 * Fuente de verdad de usuario/rol: BD. Claves: seeds + claves guardadas al editar en Personal.
 */
public class DemoCredentialStore {

    private static final Logger log = LoggerFactory.getLogger(DemoCredentialStore.class);

    private static final Map<String, String> SEED_PASSWORDS = Map.of(
            "admin", "1234",
            "empleado1", "emp123",
            "juan_prof", "prof123",
            "maria_nutri", "nutri123");

    public record DemoAccount(
            Long userId,
            String username,
            String password,
            RoleName role,
            String displayName,
            boolean active,
            boolean passwordKnown) {
    }

    private final Path storageFile;
    private final Map<Long, String> passwordsByUserId = new LinkedHashMap<>();

    public DemoCredentialStore() {
        this(Path.of(System.getProperty("user.home"), ".fitness-training", "demo-credentials.properties"));
    }

    public DemoCredentialStore(Path storageFile) {
        this.storageFile = storageFile;
        load();
    }

    public synchronized void upsert(
            Long userId,
            String username,
            String password,
            RoleName role,
            String displayName,
            boolean active) {
        rememberPassword(userId, password);
    }

    public synchronized void syncProfile(
            Long userId,
            String username,
            RoleName role,
            String displayName,
            boolean active) {
        if (userId == null || username == null) {
            return;
        }
        if (passwordsByUserId.containsKey(userId)) {
            return;
        }
        String seed = SEED_PASSWORDS.get(username.trim().toLowerCase(Locale.ROOT));
        if (seed != null) {
            rememberPassword(userId, seed);
        }
    }

    public synchronized void markInactive(Long userId) {
        // Se filtra por active en BD al listar.
    }

    public synchronized void rememberPassword(Long userId, String password) {
        if (userId == null || password == null || password.isBlank()) {
            return;
        }
        passwordsByUserId.put(userId, password);
        save();
    }

    public synchronized Optional<String> passwordOf(Long userId) {
        return Optional.ofNullable(passwordsByUserId.get(userId));
    }

    /**
     * Arma el panel desde usuarios activos de BD (rol vigente),
     * resolviendo claves conocidas (edits de Personal + seeds).
     */
    public synchronized List<DemoAccount> buildPanelAccounts(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        List<User> activeUsers = userRepository.listActive();
        Map<Long, String> resolvedPasswords = new LinkedHashMap<>();
        boolean changed = false;

        for (User user : activeUsers) {
            String password = resolvePassword(user, passwordHasher);
            if (password != null) {
                resolvedPasswords.put(user.getId(), password);
                String previous = passwordsByUserId.put(user.getId(), password);
                if (!password.equals(previous)) {
                    changed = true;
                }
            }
        }
        if (changed) {
            save();
        }

        List<DemoAccount> rows = new ArrayList<>();
        for (RoleName role : RoleName.values()) {
            List<User> withRole = activeUsers.stream()
                    .filter(user -> roleOf(user) == role)
                    .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                    .toList();

            if (withRole.isEmpty()) {
                rows.add(new DemoAccount(null, null, null, role, null, false, false));
                continue;
            }

            for (User user : withRole) {
                String password = resolvedPasswords.get(user.getId());
                rows.add(new DemoAccount(
                        user.getId(),
                        user.getUsername(),
                        password,
                        role,
                        user.getDisplayName(),
                        true,
                        password != null));
            }
        }

        activeUsers.stream()
                .filter(user -> roleOf(user) == null)
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .forEach(user -> {
                    String password = resolvedPasswords.get(user.getId());
                    rows.add(new DemoAccount(
                            user.getId(),
                            user.getUsername(),
                            password,
                            null,
                            user.getDisplayName(),
                            true,
                            password != null));
                });

        return rows;
    }

    public synchronized void reconcile(UserRepository userRepository, PasswordHasher passwordHasher) {
        buildPanelAccounts(userRepository, passwordHasher);
    }

    private String resolvePassword(User user, PasswordHasher passwordHasher) {
        String known = passwordsByUserId.get(user.getId());
        if (known != null && passwordHasher.matches(known, user.getPasswordHash())) {
            return known;
        }

        String byUsername = SEED_PASSWORDS.get(user.getUsername().toLowerCase(Locale.ROOT));
        if (byUsername != null && passwordHasher.matches(byUsername, user.getPasswordHash())) {
            return byUsername;
        }

        // Usuario renombrado que conservó la clave seed (ej. empleado1 → empleado01).
        for (String seed : SEED_PASSWORDS.values()) {
            if (passwordHasher.matches(seed, user.getPasswordHash())) {
                return seed;
            }
        }

        if (known != null) {
            passwordsByUserId.remove(user.getId());
            save();
        }
        return null;
    }

    private void load() {
        if (storageFile == null || !Files.isRegularFile(storageFile)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(storageFile)) {
            properties.load(in);
            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("user.")) {
                    continue;
                }
                try {
                    Long userId = Long.valueOf(key.substring("user.".length()));
                    String password = properties.getProperty(key);
                    if (password != null && !password.isBlank()) {
                        passwordsByUserId.put(userId, password);
                    }
                } catch (NumberFormatException ignored) {
                    // skip malformed keys
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudieron cargar claves demo desde {}", storageFile, ex);
        }
    }

    private void save() {
        if (storageFile == null) {
            return;
        }
        try {
            Files.createDirectories(storageFile.getParent());
            Properties properties = new Properties();
            for (Map.Entry<Long, String> entry : passwordsByUserId.entrySet()) {
                properties.setProperty("user." + entry.getKey(), entry.getValue());
            }
            try (OutputStream out = Files.newOutputStream(storageFile)) {
                properties.store(out, "Fitness Training — claves demo (solo desarrollo)");
            }
        } catch (IOException ex) {
            log.warn("No se pudieron guardar claves demo en {}", storageFile, ex);
        }
    }

    private static RoleName roleOf(User user) {
        String name = user.primaryRoleName();
        if (name == null) {
            return null;
        }
        try {
            return RoleName.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String labelFor(DemoAccount account) {
        String roleLabel = roleLabel(account.role());
        if (account.userId() == null) {
            return roleLabel + "  ·  sin usuario activo";
        }
        if (account.passwordKnown()) {
            return roleLabel + "  ·  " + account.username() + " / " + account.password();
        }
        return roleLabel + "  ·  " + account.username() + "  ·  clave no disponible en demo";
    }

    public static String roleLabel(RoleName role) {
        if (role == null) {
            return "Sin rol";
        }
        return switch (role) {
            case ADMIN -> "Administrador";
            case RECEPTIONIST -> "Recepción";
            case TRAINER -> "Entrenador";
            case NUTRITIONIST -> "Nutricionista";
        };
    }

    public static Map<String, String> seedPasswords() {
        return SEED_PASSWORDS;
    }
}
