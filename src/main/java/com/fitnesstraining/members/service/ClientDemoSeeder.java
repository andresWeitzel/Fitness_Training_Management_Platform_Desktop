package com.fitnesstraining.members.service;

import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(ClientDemoSeeder.class);

    private final ClientRepository clientRepository;
    private final ClientService clientService;

    public ClientDemoSeeder(ClientRepository clientRepository, ClientService clientService) {
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    public void seedIfEmpty() {
        if (clientRepository.countAll() > 0) {
            return;
        }
        var first = clientService.create(new ClientRequest(
                "12345678", "Carlos", "García", "carlos.garcia@email.com", "1123456789", "Av. Principal 100"));
        clientService.issueCard(first.id());
        clientService.issueQr(first.id());

        clientService.create(new ClientRequest(
                "87654321", "María", "López", "maria.lopez@email.com", "1198765432", "Calle 45 200"));

        var third = clientService.create(new ClientRequest(
                "22222222", "Ana", "Rodríguez", "ana.rodriguez@email.com", "2222222222", "Calle B 300"));
        clientService.issueCard(third.id());
        log.info("Clientes de desarrollo creados.");
    }
}
