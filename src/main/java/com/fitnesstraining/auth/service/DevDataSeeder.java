package com.fitnesstraining.auth.service;

import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public DevDataSeeder(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public void seedIfEmpty() {
        if (userRepository.count() > 0) {
            return;
        }

        createUser("admin", "1234", "Administrador", "admin@fitness.local", RoleName.ADMIN);
        createUser("empleado1", "emp123", "Recepción", "recepcion@fitness.local", RoleName.RECEPTIONIST);
        createUser("juan_prof", "prof123", "Juan Entrenador", "trainer@fitness.local", RoleName.TRAINER);
        createUser("maria_nutri", "nutri123", "María Nutricionista", "nutri@fitness.local", RoleName.NUTRITIONIST);
        log.info("Usuarios de desarrollo creados.");
    }

    private void createUser(String username, String password, String displayName, String email, RoleName roleName) {
        User user = new User(username, passwordHasher.hash(password), displayName, email);
        userRepository.createWithRole(user, roleName.name());
    }
}
