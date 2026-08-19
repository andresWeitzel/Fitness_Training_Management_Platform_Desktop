package com.fitnesstraining.members.dto;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;

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
        List<CredentialView> credentials
) {

    public static ClientView from(Client client, List<CredentialView> credentials) {
        return new ClientView(
                client.getId(),
                client.getDocumentNumber(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getStatus(),
                List.copyOf(credentials)
        );
    }
}
