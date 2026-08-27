package com.fitnesstraining.analytics.service;

import com.fitnesstraining.analytics.dto.DebtRow;
import com.fitnesstraining.analytics.dto.MembershipExpiringRow;
import com.fitnesstraining.analytics.dto.OccupancyDayRow;
import com.fitnesstraining.analytics.dto.RevenueRow;
import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.model.CheckIn;
import com.fitnesstraining.checkin.repository.CheckInRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.model.MembershipPlan;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.model.Payment;
import com.fitnesstraining.payments.model.PaymentMethod;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T15:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    @Mock
    private ClientMembershipRepository membershipRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CheckInRepository checkInRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                membershipRepository, paymentRepository, checkInRepository, CLOCK);
    }

    @Test
    void rejectsInvalidExpiringDays() {
        assertThrows(ValidationException.class, () -> analyticsService.listExpiringMemberships(0));
        assertThrows(ValidationException.class, () -> analyticsService.listExpiringMemberships(91));
    }

    @Test
    void listsExpiringMembershipsWithDaysUntilExpiry() {
        Client client = client(1L, "31322333", "Nicolás", "Castro");
        MembershipPlan plan = plan("Mensual");
        ClientMembership membership = membership(10L, client, plan, NOW.plusDays(5));
        when(membershipRepository.listExpiringBetween(eq(NOW), eq(NOW.plusDays(7))))
                .thenReturn(List.of(membership));

        List<MembershipExpiringRow> rows = analyticsService.listExpiringMemberships(7);

        assertEquals(1, rows.size());
        assertEquals("Castro, Nicolás", rows.getFirst().clientName());
        assertEquals(5, rows.getFirst().daysUntilExpiry());
        assertEquals(LocalDate.of(2026, 9, 1), rows.getFirst().endsOn());
        assertEquals("Pronto", rows.getFirst().urgency());
    }

    @Test
    void listsBlockingDebts() {
        Client client = client(2L, "20111222", "Ana", "López");
        Payment payment = payment(20L, client, PaymentType.LATE_FEE, new BigDecimal("1500.00"), NOW.minusDays(3));
        when(paymentRepository.listBlockingDebts(NOW)).thenReturn(List.of(payment));

        List<DebtRow> rows = analyticsService.listBlockingDebts();

        assertEquals(1, rows.size());
        assertEquals(3, rows.getFirst().daysOverdue());
        assertEquals(PaymentType.LATE_FEE, rows.getFirst().type());
        assertEquals("Alta", rows.getFirst().severity());
        verify(paymentRepository).listBlockingDebts(NOW);
    }

    @Test
    void listsRevenueInRange() {
        Client client = client(3L, "30111222", "Luis", "Pérez");
        Payment payment = paidPayment(30L, client, PaymentType.MEMBERSHIP, new BigDecimal("20000.00"),
                NOW.minusDays(2));
        when(paymentRepository.listPaidBetween(any(), any())).thenReturn(List.of(payment));

        List<RevenueRow> rows = analyticsService.listRevenue(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 27));

        assertEquals(1, rows.size());
        assertEquals(new BigDecimal("20000.00"), rows.getFirst().amount());
        assertEquals(LocalDate.of(2026, 8, 25), rows.getFirst().paidOn());
    }

    @Test
    void aggregatesOccupancyByDay() {
        Client a = client(1L, "1", "A", "Uno");
        Client b = client(2L, "2", "B", "Dos");
        OffsetDateTime day1 = LocalDate.of(2026, 8, 26).atStartOfDay().atOffset(ZoneOffset.UTC).plusHours(10);
        OffsetDateTime day2 = LocalDate.of(2026, 8, 27).atStartOfDay().atOffset(ZoneOffset.UTC).plusHours(9);
        when(checkInRepository.listBetween(any(), any())).thenReturn(List.of(
                checkIn(a, day1),
                checkIn(a, day1.plusHours(2)),
                checkIn(b, day2)));

        List<OccupancyDayRow> rows = analyticsService.listOccupancyByDay(
                LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 27));

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).entries());
        assertEquals(1, rows.get(0).uniqueClients());
        assertEquals(1, rows.get(1).entries());
        assertEquals(1, rows.get(1).uniqueClients());
    }

    private static Client client(Long id, String document, String first, String last) {
        Client client = Client.register(document, first, last, null, null, null, NOW);
        setId(client, id);
        return client;
    }

    private static MembershipPlan plan(String name) {
        return MembershipPlan.create(name, null, 30, new BigDecimal("10000"), NOW);
    }

    private static ClientMembership membership(
            Long id, Client client, MembershipPlan plan, OffsetDateTime endsAt) {
        ClientMembership membership = ClientMembership.assign(client, plan, NOW.minusDays(25), endsAt, NOW);
        setId(membership, id);
        return membership;
    }

    private static Payment payment(
            Long id, Client client, PaymentType type, BigDecimal amount, OffsetDateTime dueAt) {
        Payment payment = Payment.register(
                client, null, type, amount, null, dueAt, false, null, NOW.minusDays(10));
        setId(payment, id);
        return payment;
    }

    private static Payment paidPayment(
            Long id, Client client, PaymentType type, BigDecimal amount, OffsetDateTime paidAt) {
        Payment payment = Payment.register(
                client, null, type, amount, PaymentMethod.CASH, null, true, null, paidAt);
        setId(payment, id);
        return payment;
    }

    private static CheckIn checkIn(Client client, OffsetDateTime at) {
        return CheckIn.register(client, null, AccessMode.MEMBERSHIP, null, at);
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
