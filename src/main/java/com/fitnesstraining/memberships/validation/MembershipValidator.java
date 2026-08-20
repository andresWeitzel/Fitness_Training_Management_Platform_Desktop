package com.fitnesstraining.memberships.validation;

import com.fitnesstraining.memberships.dto.AssignMembershipRequest;
import com.fitnesstraining.memberships.dto.MembershipPlanRequest;
import com.fitnesstraining.shared.exception.ValidationException;

import java.math.BigDecimal;

public final class MembershipValidator {

    private MembershipValidator() {
    }

    public static MembershipPlanRequest normalizeAndValidatePlan(MembershipPlanRequest request) {
        if (request == null) {
            throw new ValidationException("Complete los datos del plan.");
        }
        String name = blankToNull(request.name());
        String description = blankToNull(request.description());
        Integer durationDays = request.durationDays();
        BigDecimal price = request.price() == null ? BigDecimal.ZERO : request.price();
        boolean active = request.active() == null || request.active();

        if (name == null) {
            throw new ValidationException("El nombre del plan es obligatorio.");
        }
        if (name.length() > 100) {
            throw new ValidationException("El nombre no puede superar 100 caracteres.");
        }
        if (durationDays == null || durationDays <= 0) {
            throw new ValidationException("La duración debe ser mayor a cero días.");
        }
        if (durationDays > 3650) {
            throw new ValidationException("La duración no puede superar 3650 días.");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El precio no puede ser negativo.");
        }
        if (description != null && description.length() > 500) {
            throw new ValidationException("La descripción no puede superar 500 caracteres.");
        }

        return new MembershipPlanRequest(name, description, durationDays, price, active);
    }

    public static AssignMembershipRequest normalizeAndValidateAssign(AssignMembershipRequest request) {
        if (request == null) {
            throw new ValidationException("Seleccione cliente y plan.");
        }
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        if (request.planId() == null) {
            throw new ValidationException("Seleccione un plan.");
        }
        return new AssignMembershipRequest(request.clientId(), request.planId(), request.startDate());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
