package com.fitnesstraining.nutrition.service;

import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.nutrition.dto.HealthRecordRequest;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentRequest;
import com.fitnesstraining.nutrition.model.NutritionAppointment;
import com.fitnesstraining.nutrition.model.NutritionAppointmentStatus;
import com.fitnesstraining.nutrition.model.NutritionPlan;
import com.fitnesstraining.nutrition.model.NutritionPlanStatus;
import com.fitnesstraining.nutrition.repository.HealthRecordRepository;
import com.fitnesstraining.nutrition.repository.NutritionAppointmentRepository;
import com.fitnesstraining.nutrition.repository.NutritionPlanRepository;
import com.fitnesstraining.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T15:00:00Z"), ZoneOffset.UTC);

    @Mock private NutritionAppointmentRepository appointmentRepository;
    @Mock private NutritionPlanRepository planRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AccessCredentialRepository credentialRepository;
    @Mock private UserRepository userRepository;

    private NutritionService nutritionService;

    @BeforeEach
    void setUp() {
        nutritionService = new NutritionService(
                appointmentRepository,
                planRepository,
                healthRecordRepository,
                clientRepository,
                credentialRepository,
                userRepository,
                CLOCK);
    }

    @Test
    void scheduleAppointmentPersistsScheduledStatus() {
        Client client = activeClient(1L);
        User nutritionist = activeUser(5L);
        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(5L)).thenReturn(Optional.of(nutritionist));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(appointmentRepository.save(any(NutritionAppointment.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = nutritionService.scheduleAppointment(
                new NutritionAppointmentRequest(
                        1L,
                        LocalDate.of(2026, 8, 25),
                        LocalTime.of(10, 30),
                        "Primera consulta"),
                5L);

        assertEquals(NutritionAppointmentStatus.SCHEDULED, view.status());
        ArgumentCaptor<NutritionAppointment> captor = ArgumentCaptor.forClass(NutritionAppointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertEquals(client, captor.getValue().getClient());
    }

    @Test
    void rescheduleAppointmentUpdatesDateTimeAndNotes() {
        Client client = activeClient(1L);
        NutritionAppointment appointment = withId(NutritionAppointment.createScheduled(
                client,
                activeUser(5L),
                OffsetDateTime.of(2026, 8, 25, 10, 0, 0, 0, ZoneOffset.UTC),
                "Original"), 9L);
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(appointment));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(appointmentRepository.save(any(NutritionAppointment.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = nutritionService.rescheduleAppointment(
                9L,
                new NutritionAppointmentRequest(
                        1L,
                        LocalDate.of(2026, 8, 28),
                        LocalTime.of(16, 0),
                        "Reprogramado"));

        assertEquals(NutritionAppointmentStatus.SCHEDULED, view.status());
        assertEquals("Reprogramado", view.notes());
        assertEquals(LocalDate.of(2026, 8, 28), view.scheduledAt().toLocalDate());
        assertEquals(LocalTime.of(16, 0), view.scheduledAt().toLocalTime());
    }

    @Test
    void rescheduleAppointmentRejectsNonScheduled() {
        NutritionAppointment appointment = withId(NutritionAppointment.createScheduled(
                activeClient(1L),
                activeUser(5L),
                OffsetDateTime.now(CLOCK),
                null), 9L);
        appointment.setStatus(NutritionAppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(appointment));

        assertThrows(ValidationException.class, () -> nutritionService.rescheduleAppointment(
                9L,
                new NutritionAppointmentRequest(1L, LocalDate.of(2026, 8, 28), LocalTime.of(16, 0), null)));
    }

    @Test
    void cancelAndNoShowOnlyWhenScheduled() {
        NutritionAppointment completed = withId(NutritionAppointment.createScheduled(
                activeClient(1L),
                activeUser(5L),
                OffsetDateTime.now(CLOCK),
                null), 9L);
        completed.setStatus(NutritionAppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(completed));

        assertThrows(ValidationException.class, () -> nutritionService.cancelAppointment(9L));
        assertThrows(ValidationException.class, () -> nutritionService.markNoShow(9L));
    }

    @Test
    void archivePlanMarksArchived() {
        NutritionPlan plan = withId(NutritionPlan.create(
                activeClient(1L),
                activeUser(5L),
                "Plan verano",
                null,
                null,
                NutritionPlanStatus.ACTIVE,
                LocalDate.now(CLOCK),
                null,
                null), 3L);
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(planRepository.save(any(NutritionPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = nutritionService.archivePlan(3L);

        assertEquals(NutritionPlanStatus.ARCHIVED, view.status());
    }

    @Test
    void closeForClientDeactivationCancelsAppointmentsAndArchivesPlans() {
        nutritionService.closeForClientDeactivation(1L);

        verify(appointmentRepository).cancelScheduledForClient(1L);
        verify(planRepository).archiveOpenForClient(1L);
    }

    @Test
    void healthRecordRequiresAtLeastOneField() {
        assertThrows(ValidationException.class, () -> nutritionService.addHealthRecord(
                new HealthRecordRequest(1L, LocalDate.now(CLOCK), null, null, null, null, null),
                5L));
    }

    @Test
    void completeAppointmentOnlyWhenScheduled() {
        NutritionAppointment appointment = NutritionAppointment.createScheduled(
                activeClient(1L),
                activeUser(5L),
                OffsetDateTime.now(CLOCK),
                null);
        appointment.setStatus(NutritionAppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(appointment));

        assertThrows(ValidationException.class, () -> nutritionService.completeAppointment(9L));
    }

    private static Client activeClient(Long id) {
        Client client = Client.register(
                "30111222", "Ana", "Cliente", "ana@example.com", "111", null, OffsetDateTime.now(CLOCK));
        return withId(client, id);
    }

    private static User activeUser(Long id) {
        User user = new User("maria_nutri", "hash", "María Nutricionista", "nutri@example.com");
        return withId(user, id);
    }

    private static <T> T withId(T entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
