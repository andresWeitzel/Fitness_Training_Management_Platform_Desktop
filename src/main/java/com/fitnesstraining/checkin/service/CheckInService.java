package com.fitnesstraining.checkin.service;

import com.fitnesstraining.checkin.dto.CheckInDetail;
import com.fitnesstraining.checkin.dto.CheckInEvaluation;
import com.fitnesstraining.checkin.dto.CheckInSnapshot;
import com.fitnesstraining.checkin.dto.CheckInSummary;
import com.fitnesstraining.checkin.dto.CheckInView;
import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.model.CheckIn;
import com.fitnesstraining.checkin.model.CheckInDenialReason;
import com.fitnesstraining.checkin.repository.CheckInRepository;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipStatus;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.repository.PaymentRepository;
import com.fitnesstraining.shared.exception.AppException;
import com.fitnesstraining.shared.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final ClientMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public CheckInService(
            CheckInRepository checkInRepository,
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            ClientMembershipRepository membershipRepository,
            PaymentRepository paymentRepository,
            Clock clock) {
        this.checkInRepository = checkInRepository;
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    public CheckInSnapshot snapshot() {
        DayWindow day = todayWindow();
        int entries = (int) checkInRepository.countBetween(day.from(), day.to());
        int unique = (int) checkInRepository.countDistinctClientsBetween(day.from(), day.to());
        return new CheckInSnapshot(entries, unique);
    }

    public List<CheckInSummary> listToday() {
        return listByDate(now().toLocalDate());
    }

    public List<CheckInSummary> listByDate(LocalDate date) {
        DayWindow day = windowFor(date);
        return checkInRepository.listBetween(day.from(), day.to()).stream()
                .map(this::toSummary)
                .toList();
    }

    public CheckInDetail getDetail(Long checkInId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new AppException("Ingreso no encontrado."));
        Client client = checkIn.getClient();
        OffsetDateTime now = now();
        List<CredentialView> credentials = credentialRepository.findByClientId(client.getId()).stream()
                .map(credential -> CredentialView.from(credential, now))
                .toList();
        String planName = membershipRepository.findActiveByClientId(client.getId())
                .filter(m -> m.effectiveStatus(now) == MembershipStatus.ACTIVE)
                .map(m -> m.getPlan().getName())
                .orElse(null);
        AccessCredential used = checkIn.getCredential();
        return new CheckInDetail(
                checkIn.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                client.getEmail(),
                client.getPhone(),
                credentialRepository.findClientNumber(client.getId()).orElse(null),
                checkIn.getAccessMode(),
                planName,
                checkIn.getCredentialType(),
                used == null ? null : used.getCode(),
                checkIn.getCheckedInAt(),
                checkIn.getNotes(),
                credentials);
    }

    public CheckInEvaluation evaluate(String rawTerm) {
        String term = normalize(rawTerm);
        DayWindow day = todayWindow();
        int entries = (int) checkInRepository.countBetween(day.from(), day.to());

        ResolvedAccess resolved = resolve(term);
        if (resolved == null) {
            return CheckInEvaluation.denied(
                    CheckInDenialReason.NOT_FOUND,
                    "No se encontró cliente ni credencial con ese dato.",
                    null, null, null, null, null, null,
                    entries);
        }

        Client client = resolved.client();
        AccessCredential credential = resolved.credential();
        String clientNumber = credentialRepository.findClientNumber(client.getId()).orElse(null);

        if (client.isDeleted() || client.getStatus() != ClientStatus.ACTIVE) {
            return CheckInEvaluation.denied(
                    CheckInDenialReason.CLIENT_INACTIVE,
                    "El cliente está dado de baja o inactivo.",
                    client.getId(),
                    client.getDocumentNumber(),
                    client.fullName(),
                    clientNumber,
                    credential == null ? null : credential.getType(),
                    credential == null ? null : credential.getCode(),
                    entries);
        }

        if (credential != null && !credential.isUsable(now())) {
            return CheckInEvaluation.denied(
                    CheckInDenialReason.CREDENTIAL_EXPIRED,
                    "La credencial está vencida o inactiva.",
                    client.getId(),
                    client.getDocumentNumber(),
                    client.fullName(),
                    clientNumber,
                    credential.getType(),
                    credential.getCode(),
                    entries);
        }

        if (paymentRepository.hasBlockingDebt(client.getId(), now())) {
            return CheckInEvaluation.denied(
                    CheckInDenialReason.OPEN_DEBT,
                    "Acceso bloqueado: el cliente tiene mora (pago vencido o recargo pendiente). Registre el cobro en Pagos.",
                    client.getId(),
                    client.getDocumentNumber(),
                    client.fullName(),
                    clientNumber,
                    credential == null ? null : credential.getType(),
                    credential == null ? null : credential.getCode(),
                    entries);
        }

        AccessDecision access = resolveAccess(client.getId(), day);
        if (access.mode() == null) {
            return CheckInEvaluation.denied(
                    CheckInDenialReason.NO_ACCESS,
                    "Sin membresía activa ni pase diario cobrado hoy. Registre el cobro en Pagos.",
                    client.getId(),
                    client.getDocumentNumber(),
                    client.fullName(),
                    clientNumber,
                    credential == null ? null : credential.getType(),
                    credential == null ? null : credential.getCode(),
                    entries);
        }

        boolean alreadyIn = checkInRepository.hasCheckInToday(client.getId(), day.from(), day.to());
        String message = alreadyIn
                ? "Ya ingresó hoy. Puede registrar reingreso con " + labelForMode(access.mode()) + "."
                : "Acceso permitido por " + labelForMode(access.mode()) + ".";

        return new CheckInEvaluation(
                true,
                null,
                message,
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                clientNumber,
                credential == null ? null : credential.getType(),
                credential == null ? null : credential.getCode(),
                access.mode(),
                access.planName(),
                alreadyIn,
                entries);
    }

    public CheckInView register(String rawTerm) {
        CheckInEvaluation evaluation = evaluate(rawTerm);
        if (!evaluation.allowed()) {
            throw new ValidationException(evaluation.message());
        }

        Client client = clientRepository.findActiveById(evaluation.clientId())
                .orElseThrow(() -> new ValidationException("El cliente no existe o está dado de baja."));
        AccessCredential credential = null;
        if (evaluation.credentialCode() != null) {
            credential = credentialRepository.findUsableByCode(evaluation.credentialCode(), now()).orElse(null);
        }

        CheckIn saved = checkInRepository.save(CheckIn.register(
                client,
                credential,
                evaluation.accessMode(),
                evaluation.alreadyCheckedInToday() ? "Reingreso" : null,
                now()));

        return new CheckInView(
                saved.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                saved.getAccessMode(),
                saved.getCredentialType(),
                credential == null ? null : credential.getCode(),
                saved.getCheckedInAt(),
                evaluation.alreadyCheckedInToday()
                        ? "Reingreso registrado."
                        : "Ingreso registrado correctamente.");
    }

    private ResolvedAccess resolve(String term) {
        Optional<AccessCredential> byCode = credentialRepository.findUsableByCode(term, now());
        if (byCode.isPresent()) {
            return new ResolvedAccess(byCode.get().getClient(), byCode.get());
        }

        Optional<AccessCredential> expiredOrInactiveWindow = credentialRepository.findActiveByCode(term);
        if (expiredOrInactiveWindow.isPresent()) {
            return new ResolvedAccess(expiredOrInactiveWindow.get().getClient(), expiredOrInactiveWindow.get());
        }

        return clientRepository.findActiveByDocument(term)
                .map(client -> new ResolvedAccess(client, null))
                .orElse(null);
    }

    private AccessDecision resolveAccess(Long clientId, DayWindow day) {
        Optional<ClientMembership> active = membershipRepository.findActiveByClientId(clientId);
        if (active.isPresent()) {
            ClientMembership membership = active.get();
            if (membership.effectiveStatus(now()) == MembershipStatus.ACTIVE) {
                return new AccessDecision(AccessMode.MEMBERSHIP, membership.getPlan().getName());
            }
        }
        if (paymentRepository.hasPaidDailyPassOnDay(clientId, day.from(), day.to())) {
            return new AccessDecision(AccessMode.DAILY_PASS, null);
        }
        return new AccessDecision(null, null);
    }

    private CheckInSummary toSummary(CheckIn checkIn) {
        Client client = checkIn.getClient();
        return new CheckInSummary(
                checkIn.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                checkIn.getAccessMode(),
                checkIn.getCredentialType(),
                checkIn.getCheckedInAt());
    }

    private DayWindow todayWindow() {
        return windowFor(now().toLocalDate());
    }

    private DayWindow windowFor(LocalDate day) {
        OffsetDateTime now = now();
        ZoneOffset offset = now.getOffset();
        OffsetDateTime from = day.atTime(LocalTime.MIN).atOffset(offset);
        OffsetDateTime to = day.plusDays(1).atTime(LocalTime.MIN).atOffset(offset);
        return new DayWindow(from, to);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static String normalize(String rawTerm) {
        if (rawTerm == null || rawTerm.isBlank()) {
            throw new ValidationException("Ingrese documento, n° de cliente, carnet o QR.");
        }
        return rawTerm.trim();
    }

    private static String labelForMode(AccessMode mode) {
        return switch (mode) {
            case MEMBERSHIP -> "membresía";
            case DAILY_PASS -> "pase diario";
        };
    }

    private record ResolvedAccess(Client client, AccessCredential credential) {
    }

    private record AccessDecision(AccessMode mode, String planName) {
    }

    private record DayWindow(OffsetDateTime from, OffsetDateTime to) {
    }
}
