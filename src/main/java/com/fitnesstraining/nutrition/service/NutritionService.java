package com.fitnesstraining.nutrition.service;

import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientListScope;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.nutrition.dto.HealthRecordRequest;
import com.fitnesstraining.nutrition.dto.HealthRecordSummary;
import com.fitnesstraining.nutrition.dto.HealthRecordView;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentRequest;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentSummary;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentView;
import com.fitnesstraining.nutrition.dto.NutritionClientOption;
import com.fitnesstraining.nutrition.dto.NutritionPlanRequest;
import com.fitnesstraining.nutrition.dto.NutritionPlanSummary;
import com.fitnesstraining.nutrition.dto.NutritionPlanView;
import com.fitnesstraining.nutrition.model.HealthRecordEntry;
import com.fitnesstraining.nutrition.model.NutritionAppointment;
import com.fitnesstraining.nutrition.model.NutritionAppointmentListScope;
import com.fitnesstraining.nutrition.model.NutritionAppointmentStatus;
import com.fitnesstraining.nutrition.model.NutritionPlan;
import com.fitnesstraining.nutrition.model.NutritionPlanListScope;
import com.fitnesstraining.nutrition.model.NutritionPlanStatus;
import com.fitnesstraining.nutrition.repository.HealthRecordRepository;
import com.fitnesstraining.nutrition.repository.NutritionAppointmentRepository;
import com.fitnesstraining.nutrition.repository.NutritionPlanRepository;
import com.fitnesstraining.shared.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NutritionService {

    private final NutritionAppointmentRepository appointmentRepository;
    private final NutritionPlanRepository planRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public NutritionService(
            NutritionAppointmentRepository appointmentRepository,
            NutritionPlanRepository planRepository,
            HealthRecordRepository healthRecordRepository,
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            UserRepository userRepository,
            Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.planRepository = planRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public List<NutritionClientOption> listActiveClients() {
        return clientRepository.list(ClientListScope.ACTIVE).stream()
                .sorted(Comparator.comparing(Client::fullName, String.CASE_INSENSITIVE_ORDER))
                .map(client -> new NutritionClientOption(
                        client.getId(),
                        client.getDocumentNumber(),
                        client.fullName(),
                        credentialRepository.findClientNumber(client.getId()).orElse(null)))
                .toList();
    }

    public List<NutritionAppointmentSummary> listAppointments(String term, NutritionAppointmentListScope scope) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime since = scope == NutritionAppointmentListScope.LAST_30_DAYS
                ? now.minusDays(30)
                : null;
        return appointmentRepository.list(term, scope, now, since).stream()
                .map(this::toAppointmentSummary)
                .toList();
    }

    public NutritionAppointmentView getAppointment(Long id) {
        return toAppointmentView(requireAppointment(id));
    }

    public NutritionAppointmentView scheduleAppointment(NutritionAppointmentRequest request, Long nutritionistUserId) {
        validateAppointmentRequest(request);
        Client client = requireActiveClient(request.clientId());
        User nutritionist = requireActiveUser(nutritionistUserId);

        NutritionAppointment appointment = NutritionAppointment.createScheduled(
                client,
                nutritionist,
                toScheduledAt(request.scheduledOn(), request.scheduledTime()),
                trimToNull(request.notes()));

        return toAppointmentView(appointmentRepository.save(appointment));
    }

    public NutritionAppointmentView completeAppointment(Long id) {
        NutritionAppointment appointment = requireAppointment(id);
        if (appointment.getStatus() != NutritionAppointmentStatus.SCHEDULED) {
            throw new ValidationException("Solo se pueden completar turnos programados.");
        }
        appointment.setStatus(NutritionAppointmentStatus.COMPLETED);
        return toAppointmentView(appointmentRepository.save(appointment));
    }

    public NutritionAppointmentView cancelAppointment(Long id) {
        NutritionAppointment appointment = requireAppointment(id);
        if (appointment.getStatus() != NutritionAppointmentStatus.SCHEDULED) {
            throw new ValidationException("Solo se pueden cancelar turnos programados.");
        }
        appointment.setStatus(NutritionAppointmentStatus.CANCELLED);
        return toAppointmentView(appointmentRepository.save(appointment));
    }

    public NutritionAppointmentView markNoShow(Long id) {
        NutritionAppointment appointment = requireAppointment(id);
        if (appointment.getStatus() != NutritionAppointmentStatus.SCHEDULED) {
            throw new ValidationException("Solo se pueden marcar ausentes turnos programados.");
        }
        appointment.setStatus(NutritionAppointmentStatus.NO_SHOW);
        return toAppointmentView(appointmentRepository.save(appointment));
    }

    public NutritionAppointmentView rescheduleAppointment(Long id, NutritionAppointmentRequest request) {
        validateAppointmentRequest(request);
        NutritionAppointment appointment = requireAppointment(id);
        if (appointment.getStatus() != NutritionAppointmentStatus.SCHEDULED) {
            throw new ValidationException("Solo se pueden reprogramar turnos programados.");
        }
        if (!appointment.getClient().getId().equals(request.clientId())) {
            throw new ValidationException("No se puede cambiar el cliente de un turno existente.");
        }
        appointment.setScheduledAt(toScheduledAt(request.scheduledOn(), request.scheduledTime()));
        appointment.setNotes(trimToNull(request.notes()));
        return toAppointmentView(appointmentRepository.save(appointment));
    }

    public Optional<String> activePlanTitleForClient(Long clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        return planRepository.findActiveByClientId(clientId).map(NutritionPlan::getTitle);
    }

    public List<NutritionPlanSummary> listPlans(String term, NutritionPlanListScope scope) {
        return planRepository.list(term, scope).stream()
                .map(this::toPlanSummary)
                .toList();
    }

    public NutritionPlanView getPlan(Long id) {
        return toPlanView(requirePlan(id));
    }

    public NutritionPlanView createPlan(NutritionPlanRequest request, Long authorUserId) {
        validatePlanRequest(request, true);
        Client client = requireActiveClient(request.clientId());
        User author = requireActiveUser(authorUserId);

        NutritionPlan plan = NutritionPlan.create(
                client,
                author,
                request.title().trim(),
                trimToNull(request.objectives()),
                trimToNull(request.mealGuidance()),
                request.status() == null ? NutritionPlanStatus.DRAFT : request.status(),
                request.validFrom(),
                request.validUntil(),
                trimToNull(request.notes()));

        return toPlanView(planRepository.save(plan));
    }

    public NutritionPlanView updatePlan(Long id, NutritionPlanRequest request) {
        validatePlanRequest(request, false);
        NutritionPlan plan = requirePlan(id);
        applyPlanFields(plan, request);
        return toPlanView(planRepository.save(plan));
    }

    public NutritionPlanView archivePlan(Long id) {
        NutritionPlan plan = requirePlan(id);
        plan.setStatus(NutritionPlanStatus.ARCHIVED);
        return toPlanView(planRepository.save(plan));
    }

    public void closeForClientDeactivation(Long clientId) {
        appointmentRepository.cancelScheduledForClient(clientId);
        planRepository.archiveOpenForClient(clientId);
    }

    public List<HealthRecordSummary> listHealthRecords(Long clientId) {
        return listHealthRecords(clientId, null);
    }

    public List<HealthRecordSummary> listHealthRecords(Long clientId, String term) {
        if (clientId == null) {
            return List.of();
        }
        return healthRecordRepository.listByClient(clientId, term).stream()
                .map(this::toHealthSummary)
                .toList();
    }

    public List<HealthRecordSummary> listAllHealthRecords(String term) {
        return healthRecordRepository.listAll(term).stream()
                .map(this::toHealthSummary)
                .toList();
    }

    public HealthRecordView getHealthRecord(Long id) {
        return toHealthView(requireHealthRecord(id));
    }

    public HealthRecordView addHealthRecord(HealthRecordRequest request, Long authorUserId) {
        validateHealthRecordRequest(request);
        Client client = requireActiveClient(request.clientId());
        User author = requireActiveUser(authorUserId);

        HealthRecordEntry entry = HealthRecordEntry.create(
                client,
                author,
                toRecordedAt(request.recordedOn()),
                trimToNull(request.allergies()),
                trimToNull(request.restrictions()),
                trimToNull(request.conditions()),
                trimToNull(request.medications()),
                trimToNull(request.notes()));

        return toHealthView(healthRecordRepository.save(entry));
    }

    public static String labelForAppointmentStatus(NutritionAppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> "Programado";
            case COMPLETED -> "Completado";
            case CANCELLED -> "Cancelado";
            case NO_SHOW -> "Ausente";
        };
    }

    public static String labelForPlanStatus(NutritionPlanStatus status) {
        return switch (status) {
            case DRAFT -> "Borrador";
            case ACTIVE -> "Activo";
            case ARCHIVED -> "Archivado";
        };
    }

    public static String badgeClassForAppointmentStatus(NutritionAppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> "badge-pending";
            case COMPLETED -> "badge-paid";
            case CANCELLED -> "badge-cancelled";
            case NO_SHOW -> "badge-overdue";
        };
    }

    public static String badgeClassForPlanStatus(NutritionPlanStatus status) {
        return switch (status) {
            case DRAFT -> "badge-pending";
            case ACTIVE -> "badge-paid";
            case ARCHIVED -> "badge-cancelled";
        };
    }

    private void applyPlanFields(NutritionPlan plan, NutritionPlanRequest request) {
        plan.setTitle(request.title().trim());
        plan.setObjectives(trimToNull(request.objectives()));
        plan.setMealGuidance(trimToNull(request.mealGuidance()));
        plan.setStatus(request.status() == null ? NutritionPlanStatus.DRAFT : request.status());
        plan.setValidFrom(request.validFrom());
        plan.setValidUntil(request.validUntil());
        plan.setNotes(trimToNull(request.notes()));
    }

    private void validateAppointmentRequest(NutritionAppointmentRequest request) {
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        if (request.scheduledOn() == null) {
            throw new ValidationException("Indique la fecha del turno.");
        }
        if (request.scheduledTime() == null) {
            throw new ValidationException("Indique la hora del turno.");
        }
    }

    private void validatePlanRequest(NutritionPlanRequest request, boolean creating) {
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ValidationException("Indique un título para el plan.");
        }
        if (request.validFrom() != null && request.validUntil() != null
                && request.validUntil().isBefore(request.validFrom())) {
            throw new ValidationException("La vigencia hasta no puede ser anterior al inicio.");
        }
        if (creating && request.status() == NutritionPlanStatus.ARCHIVED) {
            throw new ValidationException("No puede crear un plan ya archivado.");
        }
    }

    private void validateHealthRecordRequest(HealthRecordRequest request) {
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        if (isBlank(request.allergies())
                && isBlank(request.restrictions())
                && isBlank(request.conditions())
                && isBlank(request.medications())
                && isBlank(request.notes())) {
            throw new ValidationException("Complete al menos un campo de la ficha de salud.");
        }
    }

    private Client requireActiveClient(Long clientId) {
        return clientRepository.findActiveById(clientId)
                .filter(c -> c.getStatus() == ClientStatus.ACTIVE)
                .orElseThrow(() -> new ValidationException("Seleccione un cliente activo."));
    }

    private User requireActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ValidationException("Usuario no válido."));
    }

    private NutritionAppointment requireAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Turno no encontrado."));
    }

    private NutritionPlan requirePlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Plan no encontrado."));
    }

    private HealthRecordEntry requireHealthRecord(Long id) {
        return healthRecordRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Entrada de ficha no encontrada."));
    }

    private OffsetDateTime toScheduledAt(LocalDate date, LocalTime time) {
        LocalDate day = date == null ? LocalDate.now(clock) : date;
        LocalTime hour = time == null ? LocalTime.of(9, 0) : time;
        ZoneOffset offset = clock.getZone().getRules().getOffset(clock.instant());
        return day.atTime(hour).atOffset(offset);
    }

    private OffsetDateTime toRecordedAt(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now(clock) : date;
        ZoneOffset offset = clock.getZone().getRules().getOffset(clock.instant());
        return day.atTime(LocalTime.NOON).atOffset(offset);
    }

    private NutritionAppointmentSummary toAppointmentSummary(NutritionAppointment appointment) {
        Client client = appointment.getClient();
        return new NutritionAppointmentSummary(
                appointment.getId(),
                client.getId(),
                client.fullName(),
                client.getDocumentNumber(),
                appointment.getScheduledAt(),
                appointment.getStatus(),
                appointment.getNutritionist().getDisplayName());
    }

    private NutritionAppointmentView toAppointmentView(NutritionAppointment appointment) {
        Client client = appointment.getClient();
        return new NutritionAppointmentView(
                appointment.getId(),
                client.getId(),
                client.fullName(),
                client.getDocumentNumber(),
                credentialRepository.findClientNumber(client.getId()).orElse(null),
                appointment.getNutritionist().getId(),
                appointment.getNutritionist().getDisplayName(),
                appointment.getScheduledAt(),
                appointment.getStatus(),
                appointment.getNotes());
    }

    private NutritionPlanSummary toPlanSummary(NutritionPlan plan) {
        return new NutritionPlanSummary(
                plan.getId(),
                plan.getClient().getId(),
                plan.getClient().fullName(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getValidFrom(),
                plan.getValidUntil(),
                plan.getCreatedBy().getDisplayName());
    }

    private NutritionPlanView toPlanView(NutritionPlan plan) {
        Client client = plan.getClient();
        return new NutritionPlanView(
                plan.getId(),
                client.getId(),
                client.fullName(),
                client.getDocumentNumber(),
                credentialRepository.findClientNumber(client.getId()).orElse(null),
                plan.getCreatedBy().getId(),
                plan.getCreatedBy().getDisplayName(),
                plan.getTitle(),
                plan.getObjectives(),
                plan.getMealGuidance(),
                plan.getStatus(),
                plan.getValidFrom(),
                plan.getValidUntil(),
                plan.getNotes(),
                plan.getCreatedAt());
    }

    private HealthRecordSummary toHealthSummary(HealthRecordEntry entry) {
        return new HealthRecordSummary(
                entry.getId(),
                entry.getClient().getId(),
                entry.getClient().fullName(),
                entry.getRecordedAt(),
                preview(entry.getAllergies()),
                preview(entry.getRestrictions()),
                entry.getRecordedBy().getDisplayName());
    }

    private HealthRecordView toHealthView(HealthRecordEntry entry) {
        Client client = entry.getClient();
        return new HealthRecordView(
                entry.getId(),
                client.getId(),
                client.fullName(),
                client.getDocumentNumber(),
                credentialRepository.findClientNumber(client.getId()).orElse(null),
                entry.getRecordedBy().getId(),
                entry.getRecordedBy().getDisplayName(),
                entry.getRecordedAt(),
                entry.getAllergies(),
                entry.getRestrictions(),
                entry.getConditions(),
                entry.getMedications(),
                entry.getNotes());
    }

    private static String preview(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 48 ? trimmed : trimmed.substring(0, 45) + "…";
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
