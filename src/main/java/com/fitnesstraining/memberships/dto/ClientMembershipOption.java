package com.fitnesstraining.memberships.dto;

import java.time.OffsetDateTime;

public record ClientMembershipOption(
        Long clientId,
        String documentNumber,
        String fullName,
        String clientNumber
) {
    public String displayLabel() {
        String number = clientNumber == null || clientNumber.isBlank() ? "—" : clientNumber;
        return documentNumber + " · " + fullName + " (" + number + ")";
    }
}
