package com.fitnesstraining.assessments.service;

import com.fitnesstraining.assessments.dto.AssessmentRequest;
import com.fitnesstraining.assessments.model.PhysicalAssessment;
import com.fitnesstraining.assessments.repository.AssessmentRepository;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T15:00:00Z"), ZoneOffset.UTC);

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AccessCredentialRepository credentialRepository;
    @Mock private UserRepository userRepository;

    private AssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentService(
                assessmentRepository, clientRepository, credentialRepository, userRepository, CLOCK);
    }

    @Test
    void computeBmiReturnsNullWhenMissingInputs() {
        assertNull(AssessmentService.computeBmi(null, BigDecimal.valueOf(170)));
        assertNull(AssessmentService.computeBmi(BigDecimal.valueOf(70), null));
    }

    @Test
    void computeBmiCalculatesValue() {
        BigDecimal bmi = AssessmentService.computeBmi(BigDecimal.valueOf(72), BigDecimal.valueOf(168));
        assertEquals(new BigDecimal("25.5"), bmi);
    }

    @Test
    void registerRequiresActiveClient() {
        when(clientRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
                assessmentService.register(validRequest(), 10L));
    }

    @Test
    void registerPersistsAssessment() {
        Client client = activeClient(1L);
        User assessor = activeUser(10L);

        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(10L)).thenReturn(Optional.of(assessor));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(assessmentRepository.save(any(PhysicalAssessment.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        var view = assessmentService.register(validRequest(), 10L);

        assertEquals("Cliente, Ana", view.clientName());
        assertEquals("Carlos Entrenador", view.assessorName());
        assertNotNull(view.bmi());

        ArgumentCaptor<PhysicalAssessment> captor = ArgumentCaptor.forClass(PhysicalAssessment.class);
        verify(assessmentRepository).save(captor.capture());
        assertEquals(client, captor.getValue().getClient());
        assertEquals(assessor, captor.getValue().getAssessedBy());
        assertEquals(BigDecimal.valueOf(72), captor.getValue().getWeightKg());
    }

    @Test
    void registerRequiresAtLeastOneMeasurement() {
        AssessmentRequest request = new AssessmentRequest(
                1L, LocalDate.now(CLOCK), null, null, null, null, null, null, null);

        assertThrows(ValidationException.class, () -> assessmentService.register(request, 10L));
    }

    private static AssessmentRequest validRequest() {
        return new AssessmentRequest(
                1L,
                LocalDate.of(2026, 8, 15),
                BigDecimal.valueOf(72),
                BigDecimal.valueOf(168),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(96),
                BigDecimal.valueOf(94),
                "Control mensual");
    }

    private static Client activeClient(Long id) {
        Client client = Client.register(
                "30111222",
                "Ana",
                "Cliente",
                "ana@example.com",
                "111",
                null,
                OffsetDateTime.now(CLOCK));
        try {
            var field = Client.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(client, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return client;
    }

    private static User activeUser(Long id) {
        User user = new User("carlos_trainer", "hash", "Carlos Entrenador", "carlos@example.com");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return user;
    }
}
