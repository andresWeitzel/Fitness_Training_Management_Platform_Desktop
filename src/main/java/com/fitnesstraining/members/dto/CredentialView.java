package com.fitnesstraining.members.dto;

import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.CredentialType;

import java.time.OffsetDateTime;

public record CredentialView(
        Long id,
        CredentialType type,
        String code,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        boolean active,
        boolean expired,
        String statusLabel
) {

    public static CredentialView from(AccessCredential credential, OffsetDateTime now) {
        boolean expired = credential.isExpired(now);
        String status;
        if (!credential.isActive()) {
            status = "INACTIVA";
        } else if (expired) {
            status = "VENCIDA";
        } else {
            status = "VIGENTE";
        }
        return new CredentialView(
                credential.getId(),
                credential.getType(),
                credential.getCode(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.isActive(),
                expired,
                status
        );
    }

    public String typeLabel() {
        return switch (type) {
            case CLIENT_NUMBER -> "N° cliente";
            case CARD -> "Carnet";
            case QR -> "QR";
        };
    }
}
