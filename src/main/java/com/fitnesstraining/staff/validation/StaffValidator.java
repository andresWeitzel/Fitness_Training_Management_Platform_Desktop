package com.fitnesstraining.staff.validation;

import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.staff.dto.StaffRequest;

public final class StaffValidator {

    private StaffValidator() {
    }

    public static StaffRequest normalizeAndValidateCreate(StaffRequest request) {
        StaffRequest normalized = normalizeCommon(request, true);
        if (normalized.password() == null) {
            throw new ValidationException("La contraseña es obligatoria.");
        }
        if (normalized.password().length() < 4) {
            throw new ValidationException("La contraseña debe tener al menos 4 caracteres.");
        }
        return normalized;
    }

    public static StaffRequest normalizeAndValidateUpdate(StaffRequest request) {
        StaffRequest normalized = normalizeCommon(request, true);
        if (normalized.password() != null && normalized.password().length() < 4) {
            throw new ValidationException("La contraseña debe tener al menos 4 caracteres.");
        }
        return normalized;
    }

    private static StaffRequest normalizeCommon(StaffRequest request, boolean requireUsername) {
        if (request == null) {
            throw new ValidationException("Complete los datos del usuario.");
        }
        String username = blankToNull(request.username());
        String displayName = blankToNull(request.displayName());
        String email = blankToNull(request.email());
        String password = blankToNull(request.password());

        if (requireUsername && username == null) {
            throw new ValidationException("El usuario es obligatorio.");
        }
        if (username != null) {
            if (username.length() < 3) {
                throw new ValidationException("El usuario debe tener al menos 3 caracteres.");
            }
            if (username.length() > 80) {
                throw new ValidationException("El usuario no puede superar 80 caracteres.");
            }
            if (!username.matches("[a-zA-Z0-9._-]+")) {
                throw new ValidationException("El usuario solo admite letras, números, punto, guion y guion bajo.");
            }
        }
        if (displayName == null) {
            throw new ValidationException("El nombre para mostrar es obligatorio.");
        }
        if (displayName.length() > 150) {
            throw new ValidationException("El nombre no puede superar 150 caracteres.");
        }
        if (email != null && email.length() > 150) {
            throw new ValidationException("El email no puede superar 150 caracteres.");
        }
        if (email != null && !email.contains("@")) {
            throw new ValidationException("El email no es válido.");
        }
        if (request.role() == null) {
            throw new ValidationException("Seleccione un rol.");
        }
        return new StaffRequest(username, displayName, email, password, request.role());
    }

    public static RoleName requireRole(RoleName role) {
        if (role == null) {
            throw new ValidationException("Seleccione un rol.");
        }
        return role;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
