package com.fitnesstraining.memberships.dto;

import com.fitnesstraining.memberships.model.MembershipStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ClientMembershipView(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        Long planId,
        String planName,
        int durationDays,
        BigDecimal planPrice,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        MembershipStatus status
) {
}
