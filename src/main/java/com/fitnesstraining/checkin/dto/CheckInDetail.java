package com.fitnesstraining.checkin.dto;

import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.CredentialType;

import java.time.OffsetDateTime;
import java.util.List;

public record CheckInDetail(
        Long checkInId,
        Long clientId,
        String clientDocument,
        String clientName,
        String clientEmail,
        String clientPhone,
        String clientNumber,
        AccessMode accessMode,
        String membershipPlanName,
        CredentialType usedCredentialType,
        String usedCredentialCode,
        OffsetDateTime checkedInAt,
        String notes,
        List<CredentialView> credentials
) {
}
