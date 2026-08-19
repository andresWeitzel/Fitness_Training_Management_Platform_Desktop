package com.fitnesstraining.members.service;

import com.fitnesstraining.members.repository.ClientRepository;

public class ClientQueryService {

    private final ClientRepository clientRepository;

    public ClientQueryService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public long countActiveClients() {
        return clientRepository.countActive();
    }
}
