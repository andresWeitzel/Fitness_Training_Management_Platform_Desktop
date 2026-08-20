package com.fitnesstraining.memberships.dto;

import java.time.LocalDate;

public record AssignMembershipRequest(
        Long clientId,
        Long planId,
        LocalDate startDate
) {
}
