package com.fitnesstraining.payments.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipPlan;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.dto.RegisterPaymentRequest;
import com.fitnesstraining.payments.model.Payment;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.payments.model.PaymentType;
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
import java.time.LocalDate;
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
class PaymentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T15:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMembershipRepository membershipRepository;

    @Mock
    private AccessCredentialRepository credentialRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                clientRepository,
                membershipRepository,
                credentialRepository,
                CLOCK);
    }

    @Test
    void rejectsMembershipPaymentWithoutMembership() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                paymentService.register(new RegisterPaymentRequest(
                        1L,
                        null,
                        PaymentType.MEMBERSHIP,
                        BigDecimal.TEN,
                        PaymentMethod.CASH,
                        null,
                        true,
                        null)));
        assertEquals("Seleccione la membresía asociada al cobro.", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsPaidWithoutMethod() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                paymentService.register(new RegisterPaymentRequest(
                        1L,
                        10L,
                        PaymentType.MEMBERSHIP,
                        BigDecimal.TEN,
                        null,
                        null,
                        true,
                        null)));
        assertEquals("Seleccione el medio de pago.", ex.getMessage());
    }

    @Test
    void registersPaidMembershipPayment() {
        Client client = activeClient(1L);
        MembershipPlan plan = plan(2L);
        ClientMembership membership = withId(
                ClientMembership.assign(client, plan, NOW.minusDays(5), NOW.plusDays(25), NOW), 10L);

        when(clientRepository.findActiveById(1L)).thenReturn(Optional.of(client));
        when(membershipRepository.findById(10L)).thenReturn(Optional.of(membership));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> withId(inv.getArgument(0), 100L));

        var view = paymentService.register(new RegisterPaymentRequest(
                1L,
                10L,
                PaymentType.MEMBERSHIP,
                plan.getPrice(),
                PaymentMethod.TRANSFER,
                null,
                true,
                "Cuota mensual"));

        assertEquals(PaymentStatus.PAID, view.status());
        assertEquals(PaymentType.MEMBERSHIP, view.type());
        assertEquals(PaymentMethod.TRANSFER, view.method());
        assertEquals(100L, view.id());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void marksPendingAsPaid() {
        Client client = activeClient(1L);
        Payment pending = withId(Payment.register(
                client,
                null,
                PaymentType.DAILY_PASS,
                PaymentService.DEFAULT_DAILY_PASS_AMOUNT,
                null,
                NOW.minusDays(1),
                false,
                null,
                NOW), 50L);

        when(paymentRepository.findById(50L)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = paymentService.markPaid(50L, PaymentMethod.CASH);

        assertEquals(PaymentStatus.PAID, view.status());
        assertEquals(PaymentMethod.CASH, view.method());
        assertFalse(view.overdue());
    }

    @Test
    void cancelsPendingPayment() {
        Client client = activeClient(1L);
        Payment pending = withId(Payment.register(
                client,
                null,
                PaymentType.LATE_FEE,
                new BigDecimal("2500"),
                null,
                LocalDate.parse("2026-08-17").atStartOfDay().atOffset(ZoneOffset.UTC),
                false,
                null,
                NOW), 51L);

        when(paymentRepository.findById(51L)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = paymentService.cancel(51L);

        assertEquals(PaymentStatus.CANCELLED, view.status());
    }

    @Test
    void rejectsCancelOfPaidPayment() {
        Client client = activeClient(1L);
        Payment paid = withId(Payment.register(
                client,
                null,
                PaymentType.DAILY_PASS,
                PaymentService.DEFAULT_DAILY_PASS_AMOUNT,
                PaymentMethod.CASH,
                null,
                true,
                null,
                NOW), 52L);

        when(paymentRepository.findById(52L)).thenReturn(Optional.of(paid));

        ValidationException ex = assertThrows(ValidationException.class, () -> paymentService.cancel(52L));
        assertEquals("No se puede cancelar un pago ya cobrado.", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void detectsOpenDebt() {
        when(paymentRepository.hasBlockingDebt(eq(1L), any())).thenReturn(true);
        assertTrue(paymentService.hasOpenDebt(1L));
    }

    private static Client activeClient(Long id) {
        return withId(Client.register(
                "30111222",
                "Ana",
                "Pérez",
                "ana@example.com",
                null,
                null,
                NOW), id);
    }

    private static MembershipPlan plan(Long id) {
        return withId(MembershipPlan.create("Mensual", null, 30, new BigDecimal("25000.00"), NOW), id);
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
