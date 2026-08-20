package com.fitnesstraining.payments.dto;

public record PaymentClientOption(
        Long id,
        String documentNumber,
        String fullName,
        String clientNumber
) {
}
