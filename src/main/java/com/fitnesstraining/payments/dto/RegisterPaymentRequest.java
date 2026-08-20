package com.fitnesstraining.payments.dto;

import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterPaymentRequest(
        Long clientId,
        Long clientMembershipId,
        PaymentType type,
        BigDecimal amount,
        PaymentMethod method,
        LocalDate dueDate,
        boolean markAsPaid,
        String notes
) {
}
