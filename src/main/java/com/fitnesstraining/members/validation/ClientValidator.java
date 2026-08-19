package com.fitnesstraining.members.validation;

import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.shared.exception.ValidationException;

public final class ClientValidator {

    private ClientValidator() {
    }

    public static ClientRequest normalizeAndValidate(ClientRequest request) {
        if (request == null) {
            throw new ValidationException("Complete los datos del cliente.");
        }
        String document = blankToNull(request.documentNumber());
        String firstName = blankToNull(request.firstName());
        String lastName = blankToNull(request.lastName());
        String email = blankToNull(request.email());
        String phone = blankToNull(request.phone());
        String address = blankToNull(request.address());

        if (document == null) {
            throw new ValidationException("El documento es obligatorio.");
        }
        if (document.length() < 6 || document.length() > 20) {
            throw new ValidationException("El documento debe tener entre 6 y 20 caracteres.");
        }
        if (firstName == null) {
            throw new ValidationException("El nombre es obligatorio.");
        }
        if (lastName == null) {
            throw new ValidationException("El apellido es obligatorio.");
        }
        if (email != null && !email.contains("@")) {
            throw new ValidationException("El email no es válido.");
        }

        return new ClientRequest(document, firstName, lastName, email, phone, address);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
