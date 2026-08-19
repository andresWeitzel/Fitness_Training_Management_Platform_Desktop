package com.fitnesstraining.auth.service;

import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.shared.exception.AuthenticationException;

import java.time.OffsetDateTime;

public class AuthService {

    static final String INVALID_CREDENTIALS = "Usuario o contraseña inválidos.";
    static final String INACTIVE_OR_FORBIDDEN = "Este usuario no puede ingresar al sistema.";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public AuthenticatedUser login(String username, String password) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        User user = userRepository.findActiveByUsername(normalized)
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        if (user.getRoles().isEmpty()) {
            throw new AuthenticationException(INACTIVE_OR_FORBIDDEN);
        }

        userRepository.updateLastLogin(user.getId(), OffsetDateTime.now());
        return AuthenticatedUser.from(user);
    }
}
