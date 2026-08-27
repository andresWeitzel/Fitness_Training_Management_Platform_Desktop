package com.fitnesstraining.analytics.dto;

import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueRow(
        Long paymentId,
        Long clientId,
        String clientDocument,
        String clientName,
        PaymentType type,
        BigDecimal amount,
        PaymentMethod method,
        LocalDate paidOn
) {
}
