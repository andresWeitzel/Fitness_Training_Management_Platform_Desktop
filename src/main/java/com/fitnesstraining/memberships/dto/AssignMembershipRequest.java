package com.fitnesstraining.memberships.dto;

import com.fitnesstraining.memberships.model.MembershipBillingMode;

import java.time.LocalDate;

public record AssignMembershipRequest(
        Long clientId,
        Long planId,
        LocalDate startDate,
        MembershipBillingMode billingMode
) {
    public AssignMembershipRequest(Long clientId, Long planId, LocalDate startDate) {
        this(clientId, planId, startDate, MembershipBillingMode.PENDING);
    }
}
