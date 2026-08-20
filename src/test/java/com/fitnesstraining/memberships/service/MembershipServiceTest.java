package com.fitnesstraining.memberships.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.dto.AssignMembershipRequest;
import com.fitnesstraining.memberships.dto.MembershipPlanRequest;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipPlan;
import com.fitnesstraining.memberships.model.MembershipStatus;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.memberships.repository.MembershipPlanRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    @Mock
    private MembershipPlanRepository planRepository;

    @Mock
    private ClientMembershipRepository membershipRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccessCredentialRepository credentialRepository;

    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipService(
                planRepository,
                membershipRepository,
                clientRepository,
                credentialRepository,
                CLOCK);
    }

    @Test
    void rejectsDuplicatePlanName() {
        when(planRepository.existsName("Mensual", null)).thenReturn(true);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                membershipService.createPlan(new MembershipPlanRequest(
                        "Mensual", null, 30, BigDecimal.TEN, true)));
        assertEquals("Ya existe un plan con ese nombre.", ex.getMessage());
        verify(planRepository, never()).save(any());
    }

    @Test
    void rejectsAssignToInactiveClient() {
        Client client = activeClient(1L);
        client.deactivate(NOW);
        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                membershipService.assignMembership(new AssignMembershipRequest(1L, 2L, null)));
        assertEquals("Solo se puede asignar membresía a clientes activos.", ex.getMessage());
    }

    @Test
    void rejectsSecondActiveMembership() {
        Client client = activeClient(1L);
        MembershipPlan plan = plan(2L, 30);
        ClientMembership existing = ClientMembership.assign(client, plan, NOW.minusDays(10), NOW.plusDays(20), NOW);

        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(membershipRepository.findActiveByClientId(1L)).thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                membershipService.assignMembership(new AssignMembershipRequest(1L, 2L, null)));
        assertEquals(
                "El cliente ya tiene una membresía activa. Cancélela o renueve antes de asignar otra.",
                ex.getMessage());
    }

    @Test
    void changePlanUpdatesActiveMembership() {
        Client client = activeClient(1L);
        MembershipPlan monthly = plan(2L, 30);
        MembershipPlan quarterly = withId(
                MembershipPlan.create("Trimestral", null, 90, BigDecimal.valueOf(2000), NOW), 3L);
        ClientMembership existing = withId(
                ClientMembership.assign(client, monthly, NOW.minusDays(5), NOW.plusDays(25), NOW), 10L);

        when(membershipRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(planRepository.findById(3L)).thenReturn(Optional.of(quarterly));
        when(membershipRepository.save(any(ClientMembership.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = membershipService.changePlan(10L, 3L);

        assertEquals("Trimestral", view.planName());
        assertEquals(MembershipStatus.ACTIVE, view.status());
        assertEquals(90, view.durationDays());
        verify(membershipRepository).save(existing);
    }

    @Test
    void reassignsAfterCancel() {
        Client client = activeClient(1L);
        MembershipPlan monthly = plan(2L, 30);
        MembershipPlan quarterly = withId(
                MembershipPlan.create("Trimestral", null, 90, BigDecimal.valueOf(2000), NOW), 3L);
        ClientMembership cancelled = withId(
                ClientMembership.assign(client, monthly, NOW.minusDays(5), NOW.plusDays(25), NOW), 10L);
        cancelled.cancel(NOW);

        when(membershipRepository.findById(10L)).thenReturn(Optional.of(cancelled));
        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));
        when(planRepository.findById(3L)).thenReturn(Optional.of(quarterly));
        when(membershipRepository.findActiveByClientId(1L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(ClientMembership.class))).thenAnswer(inv -> {
            ClientMembership membership = inv.getArgument(0);
            if (membership.getId() == null) {
                return withId(membership, 11L);
            }
            return membership;
        });

        var view = membershipService.reassignMembership(10L, 3L, null);

        assertEquals(MembershipStatus.ACTIVE, view.status());
        assertEquals("Trimestral", view.planName());
    }

    private static Client activeClient(Long id) {
        Client client = Client.register("12345678", "Ana", "Garcia", null, null, null, NOW);
        return withId(client, id);
    }

    private static MembershipPlan plan(Long id, int days) {
        MembershipPlan plan = MembershipPlan.create("Mensual", null, days, BigDecimal.valueOf(1000), NOW);
        return withId(plan, id);
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
