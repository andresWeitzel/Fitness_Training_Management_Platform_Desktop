package com.fitnesstraining.memberships.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.dto.AssignMembershipRequest;
import com.fitnesstraining.memberships.dto.ClientMembershipOption;
import com.fitnesstraining.memberships.dto.ClientMembershipSummary;
import com.fitnesstraining.memberships.dto.ClientMembershipView;
import com.fitnesstraining.memberships.dto.MembershipPlanRequest;
import com.fitnesstraining.memberships.dto.MembershipPlanSummary;
import com.fitnesstraining.memberships.dto.MembershipPlanView;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipBillingMode;
import com.fitnesstraining.memberships.model.MembershipListScope;
import com.fitnesstraining.memberships.model.MembershipPlan;
import com.fitnesstraining.memberships.model.MembershipStatus;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.memberships.repository.MembershipPlanRepository;
import com.fitnesstraining.memberships.validation.MembershipValidator;
import com.fitnesstraining.payments.dto.RegisterPaymentRequest;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.payments.service.PaymentService;
import com.fitnesstraining.shared.exception.AppException;
import com.fitnesstraining.shared.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class MembershipService {

    private final MembershipPlanRepository planRepository;
    private final ClientMembershipRepository membershipRepository;
    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final PaymentService paymentService;
    private final Clock clock;

    public MembershipService(
            MembershipPlanRepository planRepository,
            ClientMembershipRepository membershipRepository,
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            PaymentService paymentService,
            Clock clock) {
        this.planRepository = planRepository;
        this.membershipRepository = membershipRepository;
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    public List<MembershipPlanSummary> listPlans() {
        return planRepository.findAll().stream()
                .map(this::toPlanSummary)
                .toList();
    }

    public List<MembershipPlanSummary> listActivePlans() {
        return planRepository.findActive().stream()
                .map(this::toPlanSummary)
                .toList();
    }

    public MembershipPlanView getPlan(Long id) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException("Plan no encontrado."));
        return toPlanView(plan);
    }

    public MembershipPlanView createPlan(MembershipPlanRequest request) {
        MembershipPlanRequest normalized = MembershipValidator.normalizeAndValidatePlan(request);
        if (planRepository.existsName(normalized.name(), null)) {
            throw new ValidationException("Ya existe un plan con ese nombre.");
        }
        OffsetDateTime now = now();
        MembershipPlan plan = MembershipPlan.create(
                normalized.name(),
                normalized.description(),
                normalized.durationDays(),
                normalized.price(),
                now);
        planRepository.save(plan);
        return toPlanView(plan);
    }

    public MembershipPlanView updatePlan(Long id, MembershipPlanRequest request) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException("Plan no encontrado."));
        MembershipPlanRequest normalized = MembershipValidator.normalizeAndValidatePlan(request);
        if (planRepository.existsName(normalized.name(), id)) {
            throw new ValidationException("Ya existe un plan con ese nombre.");
        }
        plan.update(
                normalized.name(),
                normalized.description(),
                normalized.durationDays(),
                normalized.price(),
                normalized.active(),
                now());
        planRepository.save(plan);
        return toPlanView(plan);
    }

    public List<ClientMembershipSummary> listMemberships(String term, MembershipListScope scope) {
        OffsetDateTime now = now();
        syncExpiredStatuses(now);
        List<ClientMembership> memberships = term == null || term.isBlank()
                ? membershipRepository.list(scope, now)
                : membershipRepository.search(term.trim(), scope, now);
        return memberships.stream()
                .map(m -> toMembershipSummary(m, now))
                .toList();
    }

    public ClientMembershipView getMembership(Long id) {
        OffsetDateTime now = now();
        ClientMembership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new AppException("Membresía no encontrada."));
        refreshStatusIfNeeded(membership, now);
        return toMembershipView(membership, now);
    }

    public List<ClientMembershipOption> listAssignableClients() {
        return clientRepository.findAllActiveRecords().stream()
                .filter(client -> client.getStatus() == ClientStatus.ACTIVE)
                .map(client -> new ClientMembershipOption(
                        client.getId(),
                        client.getDocumentNumber(),
                        client.fullName(),
                        credentialRepository.findClientNumber(client.getId()).orElse(null)))
                .toList();
    }

    public ClientMembershipView assignMembership(AssignMembershipRequest request) {
        AssignMembershipRequest normalized = MembershipValidator.normalizeAndValidateAssign(request);
        Client client = clientRepository.findActiveById(normalized.clientId())
                .orElseThrow(() -> new ValidationException("El cliente no existe o está dado de baja."));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new ValidationException("Solo se puede asignar membresía a clientes activos.");
        }
        MembershipPlan plan = planRepository.findById(normalized.planId())
                .orElseThrow(() -> new ValidationException("Plan no encontrado."));
        if (!plan.isActive()) {
            throw new ValidationException("El plan seleccionado no está activo.");
        }

        OffsetDateTime now = now();
        membershipRepository.findActiveByClientId(client.getId()).ifPresent(existing -> {
            refreshStatusIfNeeded(existing, now);
            if (existing.effectiveStatus(now) == MembershipStatus.ACTIVE) {
                throw new ValidationException(
                        "El cliente ya tiene una membresía activa. Cancélela o renueve antes de asignar otra.");
            }
        });

        OffsetDateTime startsAt = normalized.startDate() == null
                ? now
                : normalized.startDate().atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        if (startsAt.isBefore(now.minusDays(1))) {
            throw new ValidationException("La fecha de inicio no puede ser anterior a ayer.");
        }
        OffsetDateTime endsAt = startsAt.plusDays(plan.getDurationDays());

        ClientMembership membership = ClientMembership.assign(client, plan, startsAt, endsAt, now);
        membershipRepository.save(membership);
        billMembership(membership, plan, normalized.billingMode(), "Cobro por asignación de membresía.");
        return toMembershipView(membership, now);
    }

    public ClientMembershipView changePlan(Long membershipId, Long planId) {
        return changePlan(membershipId, planId, MembershipBillingMode.PENDING);
    }

    public ClientMembershipView changePlan(Long membershipId, Long planId, MembershipBillingMode billingMode) {
        OffsetDateTime now = now();
        ClientMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new AppException("Membresía no encontrada."));
        refreshStatusIfNeeded(membership, now);

        MembershipStatus status = membership.effectiveStatus(now);
        if (status == MembershipStatus.CANCELLED) {
            throw new ValidationException(
                    "La membresía está cancelada. Use reasignar para activar un plan nuevo.");
        }
        if (status == MembershipStatus.EXPIRED) {
            throw new ValidationException(
                    "La membresía está vencida. Use reasignar para activar un plan nuevo.");
        }

        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ValidationException("Plan no encontrado."));
        if (!plan.isActive()) {
            throw new ValidationException("El plan seleccionado no está activo.");
        }
        if (membership.getPlan().getId().equals(plan.getId())) {
            throw new ValidationException("Seleccione un plan distinto al actual.");
        }

        OffsetDateTime endsAt = now.plusDays(plan.getDurationDays());
        membership.changePlan(plan, now, endsAt, now);
        membershipRepository.save(membership);
        billMembership(membership, plan, billingMode, "Cobro por cambio de plan.");
        return toMembershipView(membership, now);
    }

    public ClientMembershipView renewMembership(Long id) {
        return renewMembership(id, MembershipBillingMode.PENDING);
    }

    public ClientMembershipView renewMembership(Long id, MembershipBillingMode billingMode) {
        OffsetDateTime now = now();
        ClientMembership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new AppException("Membresía no encontrada."));
        refreshStatusIfNeeded(membership, now);

        MembershipStatus status = membership.effectiveStatus(now);
        if (status == MembershipStatus.CANCELLED) {
            throw new ValidationException(
                    "La membresía está cancelada. Use reasignar para activar un plan nuevo.");
        }

        MembershipPlan plan = membership.getPlan();
        if (!plan.isActive()) {
            throw new ValidationException("El plan asociado ya no está activo.");
        }

        OffsetDateTime base = membership.getEndsAt().isAfter(now) ? membership.getEndsAt() : now;
        OffsetDateTime newEndsAt = base.plusDays(plan.getDurationDays());
        membership.renew(newEndsAt, now);
        membershipRepository.save(membership);
        billMembership(membership, plan, billingMode, "Cobro por renovación de membresía.");
        return toMembershipView(membership, now);
    }

    public ClientMembershipView cancelMembership(Long id) {
        OffsetDateTime now = now();
        ClientMembership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new AppException("Membresía no encontrada."));
        if (membership.getStatus() == MembershipStatus.CANCELLED) {
            throw new ValidationException("La membresía ya está cancelada.");
        }
        membership.cancel(now);
        membershipRepository.save(membership);
        return toMembershipView(membership, now);
    }

    public ClientMembershipView reassignMembership(Long previousMembershipId, Long planId, LocalDate startDate) {
        return reassignMembership(previousMembershipId, planId, startDate, MembershipBillingMode.PENDING);
    }

    public ClientMembershipView reassignMembership(
            Long previousMembershipId,
            Long planId,
            LocalDate startDate,
            MembershipBillingMode billingMode) {
        ClientMembership previous = membershipRepository.findById(previousMembershipId)
                .orElseThrow(() -> new AppException("Membresía no encontrada."));
        OffsetDateTime now = now();
        refreshStatusIfNeeded(previous, now);

        MembershipStatus status = previous.effectiveStatus(now);
        if (status == MembershipStatus.ACTIVE) {
            throw new ValidationException(
                    "La membresía sigue activa. Cambie el plan o cancele antes de reasignar.");
        }

        return assignMembership(new AssignMembershipRequest(
                previous.getClient().getId(),
                planId,
                startDate,
                billingMode));
    }

    /**
     * Alta automática sin cobro: el cobro se genera en asignación/renovación/cambio
     * explícitos desde Membresías (modo Pendiente / Cobrado).
     */
    public void assignDefaultToNewClient(Long clientId) {
        planRepository.findDefaultActive().ifPresent(plan ->
                assignMembership(new AssignMembershipRequest(
                        clientId, plan.getId(), null, MembershipBillingMode.COMPLIMENTARY)));
    }

    private void billMembership(
            ClientMembership membership,
            MembershipPlan plan,
            MembershipBillingMode billingMode,
            String notes) {
        MembershipBillingMode mode = billingMode == null ? MembershipBillingMode.PENDING : billingMode;
        if (mode == MembershipBillingMode.COMPLIMENTARY) {
            return;
        }
        boolean markPaid = mode == MembershipBillingMode.PAID;
        paymentService.register(new RegisterPaymentRequest(
                membership.getClient().getId(),
                membership.getId(),
                PaymentType.MEMBERSHIP,
                plan.getPrice(),
                PaymentMethod.CASH,
                LocalDate.now(clock),
                markPaid,
                notes));
    }

    public void cancelActiveForClient(Long clientId) {
        OffsetDateTime now = now();
        membershipRepository.findActiveByClientId(clientId).ifPresent(membership -> {
            refreshStatusIfNeeded(membership, now);
            if (membership.getStatus() != MembershipStatus.CANCELLED) {
                membership.cancel(now);
                membershipRepository.save(membership);
            }
        });
    }

    private void syncExpiredStatuses(OffsetDateTime now) {
        membershipRepository.list(MembershipListScope.ALL, now).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE && m.isExpired(now))
                .forEach(m -> {
                    m.markExpired(now);
                    membershipRepository.save(m);
                });
    }

    private MembershipPlanSummary toPlanSummary(MembershipPlan plan) {
        return new MembershipPlanSummary(
                plan.getId(),
                plan.getName(),
                plan.getDurationDays(),
                plan.getPrice(),
                plan.isActive());
    }

    private MembershipPlanView toPlanView(MembershipPlan plan) {
        return new MembershipPlanView(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getDurationDays(),
                plan.getPrice(),
                plan.isActive());
    }

    private ClientMembershipSummary toMembershipSummary(ClientMembership membership, OffsetDateTime now) {
        Client client = membership.getClient();
        MembershipStatus status = membership.effectiveStatus(now);
        return new ClientMembershipSummary(
                membership.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                membership.getPlan().getName(),
                membership.getStartsAt(),
                membership.getEndsAt(),
                status);
    }

    private ClientMembershipView toMembershipView(ClientMembership membership, OffsetDateTime now) {
        Client client = membership.getClient();
        MembershipPlan plan = membership.getPlan();
        return new ClientMembershipView(
                membership.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                plan.getId(),
                plan.getName(),
                plan.getDurationDays(),
                plan.getPrice(),
                membership.getStartsAt(),
                membership.getEndsAt(),
                membership.effectiveStatus(now));
    }

    private void refreshStatusIfNeeded(ClientMembership membership, OffsetDateTime now) {
        if (membership.getStatus() == MembershipStatus.ACTIVE && membership.isExpired(now)) {
            membership.markExpired(now);
            membershipRepository.save(membership);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
