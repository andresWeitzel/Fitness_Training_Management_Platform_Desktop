package com.fitnesstraining.checkin.service;

import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.repository.CheckInRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckInDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(CheckInDemoSeeder.class);

    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final ClientMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final CheckInRepository checkInRepository;
    private final CheckInService checkInService;

    public CheckInDemoSeeder(
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            ClientMembershipRepository membershipRepository,
            PaymentRepository paymentRepository,
            CheckInRepository checkInRepository,
            CheckInService checkInService) {
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.checkInRepository = checkInRepository;
        this.checkInService = checkInService;
    }

    public void seedIfEmpty() {
        var day = java.time.OffsetDateTime.now();
        long today = checkInRepository.countBetween(
                day.toLocalDate().atStartOfDay().atOffset(day.getOffset()),
                day.toLocalDate().plusDays(1).atStartOfDay().atOffset(day.getOffset()));
        if (today > 0) {
            log.info("Check-ins demo ya presentes hoy.");
            return;
        }

        int seeded = 0;
        for (Client client : clientRepository.findAllActiveRecords()) {
            if (client.isDeleted()
                    || paymentRepository.hasBlockingDebt(client.getId(), java.time.OffsetDateTime.now())) {
                continue;
            }
            boolean hasMembership = membershipRepository.findActiveByClientId(client.getId()).isPresent();
            if (!hasMembership) {
                continue;
            }
            String lookup = credentialRepository.findClientNumber(client.getId())
                    .orElse(client.getDocumentNumber());
            try {
                var evaluation = checkInService.evaluate(lookup);
                if (!evaluation.allowed() || evaluation.accessMode() != AccessMode.MEMBERSHIP) {
                    continue;
                }
                checkInService.register(lookup);
                seeded++;
                if (seeded >= 3) {
                    break;
                }
            } catch (RuntimeException ex) {
                log.warn("No se pudo seedear check-in para {}: {}", client.getId(), ex.getMessage());
            }
        }
        log.info("Check-ins demo creados: {}.", seeded);
    }
}
