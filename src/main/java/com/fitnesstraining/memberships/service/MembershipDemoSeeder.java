package com.fitnesstraining.memberships.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(MembershipDemoSeeder.class);

    private final ClientRepository clientRepository;
    private final ClientMembershipRepository membershipRepository;
    private final MembershipService membershipService;

    public MembershipDemoSeeder(
            ClientRepository clientRepository,
            ClientMembershipRepository membershipRepository,
            MembershipService membershipService) {
        this.clientRepository = clientRepository;
        this.membershipRepository = membershipRepository;
        this.membershipService = membershipService;
    }

    public void seedMissingForActiveClients() {
        for (Client client : clientRepository.findAllActiveRecords()) {
            if (client.isDeleted()) {
                continue;
            }
            if (membershipRepository.findActiveByClientId(client.getId()).isPresent()) {
                continue;
            }
            try {
                membershipService.assignDefaultToNewClient(client.getId());
            } catch (RuntimeException ex) {
                log.warn("No se pudo asignar membresía demo a {}: {}", client.getId(), ex.getMessage());
            }
        }
        log.info("Membresías demo verificadas.");
    }
}
