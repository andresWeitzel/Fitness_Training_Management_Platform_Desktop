package com.fitnesstraining.analytics.dto;

import com.fitnesstraining.payments.model.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtRow(
        Long paymentId,
        Long clientId,
        String clientDocument,
        String clientName,
        PaymentType type,
        BigDecimal amount,
        LocalDate dueOn,
        int daysOverdue,
        String severity
) {
}
