package com.fitnesstraining.payments.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.dto.RegisterPaymentRequest;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.payments.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(PaymentDemoSeeder.class);

    private final ClientRepository clientRepository;
    private final ClientMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public PaymentDemoSeeder(
            ClientRepository clientRepository,
            ClientMembershipRepository membershipRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService) {
        this.clientRepository = clientRepository;
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    public void seedIfEmpty() {
        List<Client> clients = clientRepository.findAllActiveRecords().stream()
                .filter(client -> !client.isDeleted())
                .filter(client -> paymentRepository.countByClientId(client.getId()) == 0)
                .toList();
        if (clients.isEmpty()) {
            log.info("Pagos demo ya presentes.");
            return;
        }

        int seeded = 0;
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            ClientMembership membership = membershipRepository.findActiveByClientId(client.getId()).orElse(null);
            try {
                for (RegisterPaymentRequest request : scenarioFor(i, client, membership)) {
                    var view = paymentService.register(request);
                    seeded++;
                    if (request.notes() != null && request.notes().contains("se cancela en seed")) {
                        paymentService.cancel(view.id());
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("No se pudo seedear pagos demo para cliente {}: {}", client.getId(), ex.getMessage());
            }
        }
        log.info("Pagos demo creados: {} registros para {} clientes.", seeded, clients.size());
    }

    private static List<RegisterPaymentRequest> scenarioFor(
            int index,
            Client client,
            ClientMembership membership) {
        Long clientId = client.getId();
        Long membershipId = membership == null ? null : membership.getId();
        BigDecimal planPrice = membership == null
                ? PaymentService.DEFAULT_DAILY_PASS_AMOUNT
                : membership.getPlan().getPrice();

        return switch (index % 4) {
            case 0 -> scenarioCleanPaidMembership(clientId, membershipId, planPrice);
            case 1 -> scenarioPaidMembershipAndOverdue(clientId, membershipId, planPrice);
            case 2 -> scenarioPendingAndDailyPass(clientId, membershipId, planPrice);
            default -> scenarioMixedHistory(clientId, membershipId, planPrice);
        };
    }

    private static List<RegisterPaymentRequest> scenarioCleanPaidMembership(
            Long clientId,
            Long membershipId,
            BigDecimal planPrice) {
        List<RegisterPaymentRequest> requests = new ArrayList<>();
        if (membershipId != null) {
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    membershipId,
                    PaymentType.MEMBERSHIP,
                    planPrice,
                    PaymentMethod.CASH,
                    null,
                    true,
                    "Membresía al día (sin deuda)"));
        } else {
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    null,
                    PaymentType.DAILY_PASS,
                    PaymentService.DEFAULT_DAILY_PASS_AMOUNT,
                    PaymentMethod.CASH,
                    null,
                    true,
                    "Pase diario al día"));
        }
        return requests;
    }

    private static List<RegisterPaymentRequest> scenarioPaidMembershipAndOverdue(
            Long clientId,
            Long membershipId,
            BigDecimal planPrice) {
        List<RegisterPaymentRequest> requests = new ArrayList<>();
        if (membershipId != null) {
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    membershipId,
                    PaymentType.MEMBERSHIP,
                    planPrice,
                    PaymentMethod.CASH,
                    null,
                    true,
                    "Pago demo de membresía"));
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    membershipId,
                    PaymentType.LATE_FEE,
                    new BigDecimal("2500.00"),
                    null,
                    LocalDate.now().minusDays(5),
                    false,
                    "Recargo demo en mora"));
        } else {
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    null,
                    PaymentType.DAILY_PASS,
                    PaymentService.DEFAULT_DAILY_PASS_AMOUNT,
                    PaymentMethod.CASH,
                    null,
                    true,
                    "Pago demo de ingreso diario"));
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    null,
                    PaymentType.LATE_FEE,
                    new BigDecimal("1500.00"),
                    null,
                    LocalDate.now().minusDays(2),
                    false,
                    "Recargo demo sin membresía"));
        }
        return requests;
    }

    private static List<RegisterPaymentRequest> scenarioPendingAndDailyPass(
            Long clientId,
            Long membershipId,
            BigDecimal planPrice) {
        List<RegisterPaymentRequest> requests = new ArrayList<>();
        if (membershipId != null) {
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    membershipId,
                    PaymentType.MEMBERSHIP,
                    planPrice,
                    null,
                    LocalDate.now().plusDays(7),
                    false,
                    "Cuota pendiente a vencer"));
        }
        requests.add(new RegisterPaymentRequest(
                clientId,
                null,
                PaymentType.DAILY_PASS,
                PaymentService.DEFAULT_DAILY_PASS_AMOUNT,
                PaymentMethod.TRANSFER,
                null,
                true,
                "Ingreso diario cobrado por transferencia"));
        requests.add(new RegisterPaymentRequest(
                clientId,
                membershipId,
                PaymentType.LATE_FEE,
                new BigDecimal("3000.00"),
                PaymentMethod.CARD,
                LocalDate.now().minusDays(10),
                true,
                "Mora regularizada con tarjeta"));
        return requests;
    }

    private static List<RegisterPaymentRequest> scenarioMixedHistory(
            Long clientId,
            Long membershipId,
            BigDecimal planPrice) {
        List<RegisterPaymentRequest> requests = new ArrayList<>();
        if (membershipId != null) {
            requests.add(new RegisterPaymentRequest(
                    clientId,
                    membershipId,
                    PaymentType.MEMBERSHIP,
                    planPrice,
                    PaymentMethod.TRANSFER,
                    null,
                    true,
                    "Renovación cobrada por transferencia"));
        }
        RegisterPaymentRequest toCancel = new RegisterPaymentRequest(
                clientId,
                membershipId,
                PaymentType.LATE_FEE,
                new BigDecimal("2000.00"),
                null,
                LocalDate.now().plusDays(3),
                false,
                "Recargo cargado por error (se cancela en seed)");
        requests.add(toCancel);
        requests.add(new RegisterPaymentRequest(
                clientId,
                null,
                PaymentType.DAILY_PASS,
                PaymentService.DEFAULT_DAILY_PASS_AMOUNT,
                PaymentMethod.OTHER,
                null,
                true,
                "Pase diario (otro medio)"));
        requests.add(new RegisterPaymentRequest(
                clientId,
                membershipId,
                PaymentType.LATE_FEE,
                new BigDecimal("1800.00"),
                null,
                LocalDate.now().minusDays(1),
                false,
                "Mora reciente pendiente"));
        return requests;
    }
}
