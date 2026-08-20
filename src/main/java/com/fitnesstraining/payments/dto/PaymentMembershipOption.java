package com.fitnesstraining.payments.dto;

import java.math.BigDecimal;

public record PaymentMembershipOption(
        Long id,
        String planName,
        BigDecimal planPrice,
        String statusLabel
) {
}
