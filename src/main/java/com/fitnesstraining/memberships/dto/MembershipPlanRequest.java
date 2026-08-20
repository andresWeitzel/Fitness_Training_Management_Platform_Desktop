package com.fitnesstraining.memberships.dto;

import java.math.BigDecimal;

public record MembershipPlanRequest(
        String name,
        String description,
        Integer durationDays,
        BigDecimal price,
        Boolean active
) {
}
