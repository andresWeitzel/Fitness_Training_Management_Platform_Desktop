package com.fitnesstraining.memberships.dto;

import java.math.BigDecimal;

public record MembershipPlanView(
        Long id,
        String name,
        String description,
        int durationDays,
        BigDecimal price,
        boolean active
) {
}
