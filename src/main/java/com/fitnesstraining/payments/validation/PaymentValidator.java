package com.fitnesstraining.payments.validation;

import com.fitnesstraining.payments.dto.RegisterPaymentRequest;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.shared.exception.ValidationException;

import java.math.BigDecimal;

public final class PaymentValidator {

    private PaymentValidator() {
    }

    public static RegisterPaymentRequest normalizeAndValidate(RegisterPaymentRequest request) {
        if (request == null) {
            throw new ValidationException("Complete los datos del pago.");
        }
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        if (request.type() == null) {
            throw new ValidationException("Seleccione el tipo de pago.");
        }
        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El monto no puede ser negativo.");
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("El monto debe ser mayor a cero.");
        }
        if (request.markAsPaid() && request.method() == null) {
            throw new ValidationException("Seleccione el medio de pago.");
        }
        if (request.type() == PaymentType.MEMBERSHIP && request.clientMembershipId() == null) {
            throw new ValidationException("Seleccione la membresía asociada al cobro.");
        }

        String notes = blankToNull(request.notes());
        if (notes != null && notes.length() > 500) {
            throw new ValidationException("Las notas no pueden superar 500 caracteres.");
        }

        return new RegisterPaymentRequest(
                request.clientId(),
                request.clientMembershipId(),
                request.type(),
                amount,
                request.method(),
                request.dueDate(),
                request.markAsPaid(),
                notes);
    }

    public static PaymentMethod requireMethod(PaymentMethod method) {
        if (method == null) {
            throw new ValidationException("Seleccione el medio de pago.");
        }
        return method;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
