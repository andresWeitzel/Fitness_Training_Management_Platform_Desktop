package com.fitnesstraining.analytics.service;

import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.model.CheckIn;
import com.fitnesstraining.checkin.repository.CheckInRepository;
import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipListScope;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.model.Payment;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.payments.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Completa muestras para que Analytics no quede vacío en bases demo ya existentes:
 * vencimientos próximos, mora, ingresos repartidos en el mes y ocupación de las últimas semanas.
 */
public class AnalyticsDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsDemoSeeder.class);
    private static final String MARKER = "Analytics demo";

    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final ClientMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final CheckInRepository checkInRepository;
    private final Clock clock;

    public AnalyticsDemoSeeder(
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            ClientMembershipRepository membershipRepository,
            PaymentRepository paymentRepository,
            CheckInRepository checkInRepository,
            Clock clock) {
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.checkInRepository = checkInRepository;
        this.clock = clock;
    }

    public void seedMissingSamples() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        int memberships = ensureNearExpiringMemberships(now);
        int debts = ensureBlockingDebts(now);
        int revenue = ensureRecentRevenue(now);
        int occupancy = ensureRecentOccupancy(now);
        log.info(
                "Analytics demo: {} membresías próximas, {} deudas, {} cobros, {} check-ins.",
                memberships, debts, revenue, occupancy);
    }

    private int ensureNearExpiringMemberships(OffsetDateTime now) {
        OffsetDateTime withinTwoWeeks = now.plusDays(14);
        List<ClientMembership> alreadyNear = membershipRepository.listExpiringBetween(now, withinTwoWeeks);
        if (alreadyNear.size() >= 8) {
            return 0;
        }

        int[] offsets = {1, 2, 3, 4, 5, 6, 8, 10, 12, 14, 18, 21};
        List<ClientMembership> active = membershipRepository.list(MembershipListScope.ACTIVE, now);
        int adjusted = 0;
        for (ClientMembership membership : active) {
            if (adjusted >= offsets.length) {
                break;
            }
            if (!membership.getEndsAt().isAfter(withinTwoWeeks)) {
                continue;
            }
            membership.renew(now.plusDays(offsets[adjusted]), now);
            membershipRepository.save(membership);
            adjusted++;
        }
        return adjusted;
    }

    private int ensureBlockingDebts(OffsetDateTime now) {
        int existing = paymentRepository.listBlockingDebts(now).size();
        if (existing >= 5) {
            return 0;
        }

        List<Client> clients = activeClients();
        int created = 0;
        int[] overdueDays = {12, 8, 5, 3, 1};
        BigDecimal[] amounts = {
                new BigDecimal("4500.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("1800.00")
        };
        int target = overdueDays.length - existing;
        for (int i = 0; i < clients.size() && created < target; i++) {
            Client client = clients.get(i);
            if (paymentRepository.hasBlockingDebt(client.getId(), now)) {
                continue;
            }
            ClientMembership membership = membershipRepository.findActiveByClientId(client.getId()).orElse(null);
            int slot = existing + created;
            Payment payment = Payment.register(
                    client,
                    membership,
                    PaymentType.LATE_FEE,
                    amounts[slot % amounts.length],
                    null,
                    now.minusDays(overdueDays[slot % overdueDays.length]),
                    false,
                    MARKER + " · mora pendiente",
                    now);
            paymentRepository.save(payment);
            created++;
        }
        return created;
    }

    private int ensureRecentRevenue(OffsetDateTime now) {
        OffsetDateTime from = now.minusDays(30);
        if (paymentRepository.listPaidBetween(from, now.plusDays(1)).size() >= 16) {
            return 0;
        }

        List<Client> clients = activeClients();
        if (clients.isEmpty()) {
            return 0;
        }

        int[] daysAgo = {1, 2, 3, 5, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28};
        PaymentMethod[] methods = {
                PaymentMethod.CASH, PaymentMethod.TRANSFER, PaymentMethod.CARD, PaymentMethod.OTHER
        };
        PaymentType[] types = {
                PaymentType.MEMBERSHIP, PaymentType.DAILY_PASS, PaymentType.MEMBERSHIP, PaymentType.LATE_FEE
        };
        int created = 0;
        for (int i = 0; i < daysAgo.length; i++) {
            Client client = clients.get(i % clients.size());
            ClientMembership membership = membershipRepository.findActiveByClientId(client.getId()).orElse(null);
            PaymentType type = types[i % types.length];
            if (type == PaymentType.MEMBERSHIP && membership == null) {
                type = PaymentType.DAILY_PASS;
            }
            BigDecimal amount = type == PaymentType.DAILY_PASS
                    ? new BigDecimal("4500.00")
                    : type == PaymentType.LATE_FEE
                    ? new BigDecimal("2000.00")
                    : membership == null || membership.getPlan() == null
                    ? new BigDecimal("25000.00")
                    : membership.getPlan().getPrice();
            OffsetDateTime paidAt = now.minusDays(daysAgo[i]).withHour(11).withMinute(30).withSecond(0).withNano(0);
            Payment payment = Payment.register(
                    client,
                    type == PaymentType.DAILY_PASS ? null : membership,
                    type,
                    amount,
                    methods[i % methods.length],
                    null,
                    true,
                    MARKER + " · cobro histórico",
                    paidAt);
            paymentRepository.save(payment);
            created++;
        }
        return created;
    }

    private int ensureRecentOccupancy(OffsetDateTime now) {
        OffsetDateTime from = now.minusDays(13).toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
        OffsetDateTime to = now.plusDays(1).toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
        if (checkInRepository.countBetween(from, to) >= 28) {
            return 0;
        }

        List<Client> eligible = activeClients().stream()
                .filter(client -> !paymentRepository.hasBlockingDebt(client.getId(), now))
                .filter(client -> membershipRepository.findActiveByClientId(client.getId()).isPresent())
                .toList();
        if (eligible.isEmpty()) {
            return 0;
        }

        int[] daysAgo = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        int[] hourOffsets = {8, 9, 10, 12, 14, 17, 19};
        int created = 0;
        for (int d = 0; d < daysAgo.length; d++) {
            int visits = 2 + (d % 3);
            for (int v = 0; v < visits; v++) {
                Client client = eligible.get((d * 3 + v) % eligible.size());
                AccessCredential credential = credentialRepository.findByClientId(client.getId()).stream()
                        .findFirst()
                        .orElse(null);
                OffsetDateTime at = now.minusDays(daysAgo[d])
                        .withHour(hourOffsets[v % hourOffsets.length])
                        .withMinute(15)
                        .withSecond(0)
                        .withNano(0);
                checkInRepository.save(CheckIn.register(
                        client,
                        credential,
                        AccessMode.MEMBERSHIP,
                        MARKER + " · ocupación",
                        at));
                created++;
            }
        }
        return created;
    }

    private List<Client> activeClients() {
        return clientRepository.findAllActiveRecords().stream()
                .filter(client -> !client.isDeleted())
                .toList();
    }
}
