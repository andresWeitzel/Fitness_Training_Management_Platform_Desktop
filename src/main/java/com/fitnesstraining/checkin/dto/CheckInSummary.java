package com.fitnesstraining.checkin.dto;

import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.members.model.CredentialType;

import java.time.OffsetDateTime;

public record CheckInSummary(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        AccessMode accessMode,
        CredentialType credentialType,
        OffsetDateTime checkedInAt
) {
}
