package com.fitnesstraining.members.service;

import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.dto.DashboardSnapshot;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;

import java.util.List;

public class ClientQueryService {

    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;

    public ClientQueryService(ClientRepository clientRepository, AccessCredentialRepository credentialRepository) {
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
    }

    public long countActiveClients() {
        return clientRepository.countActive();
    }

    public DashboardSnapshot loadSnapshot() {
        List<ClientSummary> recent = clientRepository.findRecent(6).stream()
                .map(client -> ClientSummary.from(
                        client,
                        credentialRepository.findClientNumber(client.getId()).orElse("")))
                .toList();
        return new DashboardSnapshot(
                clientRepository.countActive(),
                clientRepository.countInactive(),
                credentialRepository.countActiveByType(CredentialType.CARD),
                credentialRepository.countActiveByType(CredentialType.QR),
                recent
        );
    }
}
