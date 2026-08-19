package com.fitnesstraining.app;

import com.fitnesstraining.auth.dto.AuthenticatedUser;

import java.util.Optional;

public class SessionContext {

    private AuthenticatedUser currentUser;

    public void start(AuthenticatedUser user) {
        this.currentUser = user;
    }

    public void clear() {
        this.currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public Optional<AuthenticatedUser> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public AuthenticatedUser requireUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No hay una sesión activa.");
        }
        return currentUser;
    }
}
