package com.fitnesstraining.payments.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipListScope;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.dto.PaymentClientOption;
import com.fitnesstraining.payments.dto.PaymentMembershipOption;
import com.fitnesstraining.payments.dto.PaymentSummary;
import com.fitnesstraining.payments.dto.PaymentView;
import com.fitnesstraining.payments.dto.RegisterPaymentRequest;
import com.fitnesstraining.payments.model.Payment;
import com.fitnesstraining.payments.model.PaymentListScope;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.payments.repository.PaymentRepository;
import com.fitnesstraining.payments.validation.PaymentValidator;
import com.fitnesstraining.shared.exception.AppException;
import com.fitnesstraining.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class PaymentService {

    public static final BigDecimal DEFAULT_DAILY_PASS_AMOUNT = new BigDecimal("5000.00");

    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final ClientMembershipRepository membershipRepository;
    private final AccessCredentialRepository credentialRepository;
    private final Clock clock;

    public PaymentService(
            PaymentRepository paymentRepository,
            ClientRepository clientRepository,
            ClientMembershipRepository membershipRepository,
            AccessCredentialRepository credentialRepository,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.clientRepository = clientRepository;
        this.membershipRepository = membershipRepository;
        this.credentialRepository = credentialRepository;
        this.clock = clock;
    }

    public List<PaymentSummary> list(String term, PaymentListScope scope) {
        OffsetDateTime now = now();
        List<Payment> payments = term == null || term.isBlank()
                ? paymentRepository.list(scope, now)
                : paymentRepository.search(term.trim(), scope, now);
        return payments.stream()
                .map(p -> toSummary(p, now))
                .toList();
    }

    public PaymentView get(Long id) {
        OffsetDateTime now = now();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException("Pago no encontrado."));
        return toView(payment, now);
    }

    public List<PaymentClientOption> listPayableClients() {
        return clientRepository.findAllActiveRecords().stream()
                .filter(client -> client.getStatus() == ClientStatus.ACTIVE)
                .map(client -> new PaymentClientOption(
                        client.getId(),
                        client.getDocumentNumber(),
                        client.fullName(),
                        credentialRepository.findClientNumber(client.getId()).orElse(null)))
                .toList();
    }

    public List<PaymentMembershipOption> listMembershipOptions(Long clientId) {
        OffsetDateTime now = now();
        return membershipRepository.list(MembershipListScope.ALL, now).stream()
                .filter(m -> m.getClient().getId().equals(clientId))
                .map(m -> new PaymentMembershipOption(
                        m.getId(),
                        m.getPlan().getName(),
                        m.getPlan().getPrice(),
                        labelForMembershipStatus(m.effectiveStatus(now).name())))
                .toList();
    }

    public BigDecimal suggestAmount(PaymentType type, Long clientMembershipId) {
        if (type == PaymentType.DAILY_PASS) {
            return DEFAULT_DAILY_PASS_AMOUNT;
        }
        if (clientMembershipId == null) {
            return BigDecimal.ZERO;
        }
        return membershipRepository.findById(clientMembershipId)
                .map(m -> m.getPlan().getPrice())
                .orElse(BigDecimal.ZERO);
    }

    public PaymentView register(RegisterPaymentRequest request) {
        RegisterPaymentRequest normalized = PaymentValidator.normalizeAndValidate(request);
        Client client = clientRepository.findActiveById(normalized.clientId())
                .orElseThrow(() -> new ValidationException("El cliente no existe o está dado de baja."));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new ValidationException("Solo se pueden registrar pagos de clientes activos.");
        }

        ClientMembership membership = null;
        if (normalized.clientMembershipId() != null) {
            membership = membershipRepository.findById(normalized.clientMembershipId())
                    .orElseThrow(() -> new ValidationException("Membresía no encontrada."));
            if (!membership.getClient().getId().equals(client.getId())) {
                throw new ValidationException("La membresía no pertenece al cliente seleccionado.");
            }
        }

        OffsetDateTime now = now();
        OffsetDateTime dueAt = normalized.dueDate() == null
                ? null
                : normalized.dueDate().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        Payment payment = Payment.register(
                client,
                membership,
                normalized.type(),
                normalized.amount(),
                normalized.method(),
                dueAt,
                normalized.markAsPaid(),
                normalized.notes(),
                now);
        paymentRepository.save(payment);
        return toView(payment, now);
    }

    public PaymentView markPaid(Long id, PaymentMethod method) {
        PaymentMethod required = PaymentValidator.requireMethod(method);
        OffsetDateTime now = now();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException("Pago no encontrado."));
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new ValidationException("No se puede cobrar un pago cancelado.");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ValidationException("El pago ya está cobrado.");
        }
        payment.markPaid(required, now);
        paymentRepository.save(payment);
        return toView(payment, now);
    }

    public PaymentView cancel(Long id) {
        OffsetDateTime now = now();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException("Pago no encontrado."));
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new ValidationException("El pago ya está cancelado.");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ValidationException("No se puede cancelar un pago ya cobrado.");
        }
        payment.cancel(now);
        paymentRepository.save(payment);
        return toView(payment, now);
    }

    public boolean hasOpenDebt(Long clientId) {
        return paymentRepository.hasBlockingDebt(clientId, now());
    }

    public boolean hasBlockingDebt(Long clientId) {
        return paymentRepository.hasBlockingDebt(clientId, now());
    }

    private PaymentSummary toSummary(Payment payment, OffsetDateTime now) {
        Client client = payment.getClient();
        boolean overdue = payment.isOverdue(now);
        return new PaymentSummary(
                payment.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                payment.getType(),
                payment.getAmount(),
                payment.getStatus(),
                overdue,
                payment.getMethod(),
                payment.getDueAt(),
                payment.getPaidAt());
    }

    private PaymentView toView(Payment payment, OffsetDateTime now) {
        Client client = payment.getClient();
        ClientMembership membership = payment.getClientMembership();
        return new PaymentView(
                payment.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                membership == null ? null : membership.getId(),
                membership == null ? null : membership.getPlan().getName(),
                payment.getType(),
                payment.getStatus(),
                payment.isOverdue(now),
                payment.getAmount(),
                payment.getMethod(),
                payment.getDueAt(),
                payment.getPaidAt(),
                payment.getNotes());
    }

    private static String labelForMembershipStatus(String status) {
        return switch (status) {
            case "ACTIVE" -> "Activa";
            case "EXPIRED" -> "Vencida";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
