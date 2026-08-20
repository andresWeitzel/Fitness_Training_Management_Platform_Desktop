package com.fitnesstraining.checkin.dto;

import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.model.CheckInDenialReason;
import com.fitnesstraining.members.model.CredentialType;

public record CheckInEvaluation(
        boolean allowed,
        CheckInDenialReason denialReason,
        String message,
        Long clientId,
        String clientDocument,
        String clientName,
        String clientNumber,
        CredentialType credentialType,
        String credentialCode,
        AccessMode accessMode,
        String membershipPlanName,
        boolean alreadyCheckedInToday,
        int todayEntries
) {
    public static CheckInEvaluation denied(
            CheckInDenialReason reason,
            String message,
            Long clientId,
            String clientDocument,
            String clientName,
            String clientNumber,
            CredentialType credentialType,
            String credentialCode,
            int todayEntries) {
        return new CheckInEvaluation(
                false,
                reason,
                message,
                clientId,
                clientDocument,
                clientName,
                clientNumber,
                credentialType,
                credentialCode,
                null,
                null,
                false,
                todayEntries);
    }
}
