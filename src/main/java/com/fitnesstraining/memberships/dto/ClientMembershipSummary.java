package com.fitnesstraining.memberships.dto;

import com.fitnesstraining.memberships.model.MembershipStatus;

import java.time.OffsetDateTime;

public record ClientMembershipSummary(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        String planName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        MembershipStatus status
) {
}
