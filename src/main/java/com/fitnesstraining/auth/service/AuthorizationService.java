package com.fitnesstraining.auth.service;

import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.shared.exception.AccessDeniedException;

public class AuthorizationService {

    public boolean hasPermission(AuthenticatedUser user, PermissionCode code) {
        return user != null && user.hasPermission(code);
    }

    public boolean hasAny(AuthenticatedUser user, PermissionCode... codes) {
        return user != null && user.hasAnyPermission(codes);
    }

    public void require(AuthenticatedUser user, PermissionCode code) {
        if (!hasPermission(user, code)) {
            throw new AccessDeniedException("No tiene permiso para " + code.name() + ".");
        }
    }
}
