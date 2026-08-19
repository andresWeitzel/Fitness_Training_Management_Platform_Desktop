package com.fitnesstraining.members.dto;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;

public record ClientSummary(
        Long id,
        String documentNumber,
        String fullName,
        String phone,
        ClientStatus status,
        String clientNumber
) {

    public static ClientSummary from(Client client, String clientNumber) {
        return new ClientSummary(
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                client.getPhone() == null ? "" : client.getPhone(),
                client.getStatus(),
                clientNumber == null ? "" : clientNumber
        );
    }
}
