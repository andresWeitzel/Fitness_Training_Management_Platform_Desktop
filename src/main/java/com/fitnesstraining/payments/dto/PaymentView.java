package com.fitnesstraining.payments.dto;

import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.payments.model.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentView(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        Long clientMembershipId,
        String membershipPlanName,
        PaymentType type,
        PaymentStatus status,
        boolean overdue,
        BigDecimal amount,
        PaymentMethod method,
        OffsetDateTime dueAt,
        OffsetDateTime paidAt,
        String notes
) {
}
