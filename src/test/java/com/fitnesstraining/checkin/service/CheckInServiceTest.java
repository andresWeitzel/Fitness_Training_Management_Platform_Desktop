package com.fitnesstraining.checkin.service;

import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.model.CheckIn;
import com.fitnesstraining.checkin.model.CheckInDenialReason;
import com.fitnesstraining.checkin.repository.CheckInRepository;
import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipPlan;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.repository.PaymentRepository;
import com.fitnesstraining.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T15:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    @Mock private CheckInRepository checkInRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AccessCredentialRepository credentialRepository;
    @Mock private ClientMembershipRepository membershipRepository;
    @Mock private PaymentRepository paymentRepository;

    private CheckInService checkInService;

    @BeforeEach
    void setUp() {
        checkInService = new CheckInService(
                checkInRepository,
                clientRepository,
                credentialRepository,
                membershipRepository,
                paymentRepository,
                CLOCK);
    }

    @Test
    void blocksWhenClientHasOpenDebt() {
        Client client = activeClient(1L);
        when(credentialRepository.findUsableByCode("30111222", NOW)).thenReturn(Optional.empty());
        when(credentialRepository.findActiveByCode("30111222")).thenReturn(Optional.empty());
        when(clientRepository.findActiveByDocument("30111222")).thenReturn(Optional.of(client));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(paymentRepository.hasOpenDebt(1L)).thenReturn(true);
        when(checkInRepository.countBetween(any(), any())).thenReturn(0L);

        var evaluation = checkInService.evaluate("30111222");

        assertFalse(evaluation.allowed());
        assertEquals(CheckInDenialReason.OPEN_DEBT, evaluation.denialReason());
    }

    @Test
    void allowsActiveMembershipWithoutDebt() {
        Client client = activeClient(1L);
        MembershipPlan plan = withId(MembershipPlan.create("Mensual", null, 30, BigDecimal.TEN, NOW), 2L);
        ClientMembership membership = ClientMembership.assign(client, plan, NOW.minusDays(5), NOW.plusDays(25), NOW);

        when(credentialRepository.findUsableByCode("CLI-000001", NOW)).thenReturn(Optional.of(
                credential(client, CredentialType.CLIENT_NUMBER, "CLI-000001")));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(paymentRepository.hasOpenDebt(1L)).thenReturn(false);
        when(membershipRepository.findActiveByClientId(1L)).thenReturn(Optional.of(membership));
        when(checkInRepository.countBetween(any(), any())).thenReturn(0L);
        when(checkInRepository.hasCheckInToday(eq(1L), any(), any())).thenReturn(false);

        var evaluation = checkInService.evaluate("CLI-000001");

        assertTrue(evaluation.allowed());
        assertEquals(AccessMode.MEMBERSHIP, evaluation.accessMode());
        assertEquals("Mensual", evaluation.membershipPlanName());
    }

    @Test
    void allowsDailyPassWhenPaidToday() {
        Client client = activeClient(1L);
        when(clientRepository.findActiveByDocument("30111222")).thenReturn(Optional.of(client));
        when(credentialRepository.findUsableByCode("30111222", NOW)).thenReturn(Optional.empty());
        when(credentialRepository.findActiveByCode("30111222")).thenReturn(Optional.empty());
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.empty());
        when(paymentRepository.hasOpenDebt(1L)).thenReturn(false);
        when(membershipRepository.findActiveByClientId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.hasPaidDailyPassOnDay(eq(1L), any(), any())).thenReturn(true);
        when(checkInRepository.countBetween(any(), any())).thenReturn(0L);
        when(checkInRepository.hasCheckInToday(eq(1L), any(), any())).thenReturn(false);

        var evaluation = checkInService.evaluate("30111222");

        assertTrue(evaluation.allowed());
        assertEquals(AccessMode.DAILY_PASS, evaluation.accessMode());
    }

    @Test
    void registerPersistsWhenAllowed() {
        Client client = activeClient(1L);
        MembershipPlan plan = withId(MembershipPlan.create("Mensual", null, 30, BigDecimal.TEN, NOW), 2L);
        ClientMembership membership = ClientMembership.assign(client, plan, NOW.minusDays(5), NOW.plusDays(25), NOW);
        AccessCredential credential = credential(client, CredentialType.QR, "QR-000010");

        when(credentialRepository.findUsableByCode("QR-000010", NOW)).thenReturn(Optional.of(credential));
        when(credentialRepository.findClientNumber(1L)).thenReturn(Optional.of("CLI-000001"));
        when(paymentRepository.hasOpenDebt(1L)).thenReturn(false);
        when(membershipRepository.findActiveByClientId(1L)).thenReturn(Optional.of(membership));
        when(checkInRepository.countBetween(any(), any())).thenReturn(0L);
        when(checkInRepository.hasCheckInToday(eq(1L), any(), any())).thenReturn(false);
        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> withId(inv.getArgument(0), 99L));

        var view = checkInService.register("QR-000010");

        assertEquals(99L, view.id());
        assertEquals(AccessMode.MEMBERSHIP, view.accessMode());
        verify(checkInRepository).save(any(CheckIn.class));
    }

    @Test
    void registerThrowsWhenDenied() {
        when(credentialRepository.findUsableByCode("x", NOW)).thenReturn(Optional.empty());
        when(credentialRepository.findActiveByCode("x")).thenReturn(Optional.empty());
        when(clientRepository.findActiveByDocument("x")).thenReturn(Optional.empty());
        when(checkInRepository.countBetween(any(), any())).thenReturn(0L);

        assertThrows(ValidationException.class, () -> checkInService.register("x"));
        verify(checkInRepository, never()).save(any());
    }

    private static Client activeClient(Long id) {
        return withId(Client.register("30111222", "Ana", "Pérez", null, null, null, NOW), id);
    }

    private static AccessCredential credential(Client client, CredentialType type, String code) {
        AccessCredential credential = AccessCredential.issue(type, code, NOW.minusDays(1), NOW.plusDays(30));
        credential.assignTo(client);
        return credential;
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
