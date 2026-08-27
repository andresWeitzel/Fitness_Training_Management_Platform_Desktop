package com.fitnesstraining.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MembershipExpiringRow(
        Long membershipId,
        Long clientId,
        String clientDocument,
        String clientName,
        String planName,
        BigDecimal planPrice,
        LocalDate endsOn,
        int daysUntilExpiry,
        String urgency
) {
}
