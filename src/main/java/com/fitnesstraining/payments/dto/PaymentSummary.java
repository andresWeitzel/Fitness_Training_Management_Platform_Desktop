package com.fitnesstraining.payments.dto;

import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.payments.model.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentSummary(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        PaymentType type,
        BigDecimal amount,
        PaymentStatus status,
        boolean overdue,
        PaymentMethod method,
        OffsetDateTime dueAt,
        OffsetDateTime paidAt
) {
}
