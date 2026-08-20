package com.fitnesstraining.memberships.dto;

import java.math.BigDecimal;

public record MembershipPlanSummary(
        Long id,
        String name,
        int durationDays,
        BigDecimal price,
        boolean active
) {
}
