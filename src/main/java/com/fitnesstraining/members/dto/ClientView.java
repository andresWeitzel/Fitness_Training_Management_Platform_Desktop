package com.fitnesstraining.members.dto;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.memberships.model.MembershipStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ClientView(
        Long id,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        ClientStatus status,
        List<CredentialView> credentials,
        String membershipPlanName,
        LocalDate membershipEndsOn,
        MembershipStatus membershipStatus,
        boolean hasBlockingDebt,
        OffsetDateTime lastCheckInAt,
        String activeRoutineTitle,
        String activeRoutineFocus,
        String lastAssessmentSummary,
        String activeNutritionPlanTitle
) {

    public static ClientView from(Client client, List<CredentialView> credentials) {
        return from(client, credentials, null, null, null, false, null, null, null, null, null);
    }

    public static ClientView from(
            Client client,
            List<CredentialView> credentials,
            String membershipPlanName,
            LocalDate membershipEndsOn,
            MembershipStatus membershipStatus,
            boolean hasBlockingDebt,
            OffsetDateTime lastCheckInAt,
            String activeRoutineTitle,
            String activeRoutineFocus) {
        return from(
                client,
                credentials,
                membershipPlanName,
                membershipEndsOn,
                membershipStatus,
                hasBlockingDebt,
                lastCheckInAt,
                activeRoutineTitle,
                activeRoutineFocus,
                null,
                null);
    }

    public static ClientView from(
            Client client,
            List<CredentialView> credentials,
            String membershipPlanName,
            LocalDate membershipEndsOn,
            MembershipStatus membershipStatus,
            boolean hasBlockingDebt,
            OffsetDateTime lastCheckInAt,
            String activeRoutineTitle,
            String activeRoutineFocus,
            String lastAssessmentSummary,
            String activeNutritionPlanTitle) {
        return new ClientView(
                client.getId(),
                client.getDocumentNumber(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getStatus(),
                List.copyOf(credentials),
                membershipPlanName,
                membershipEndsOn,
                membershipStatus,
                hasBlockingDebt,
                lastCheckInAt,
                activeRoutineTitle,
                activeRoutineFocus,
                lastAssessmentSummary,
                activeNutritionPlanTitle
        );
    }
}
