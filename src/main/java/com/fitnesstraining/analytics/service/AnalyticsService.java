package com.fitnesstraining.analytics.service;

import com.fitnesstraining.analytics.dto.DebtRow;
import com.fitnesstraining.analytics.dto.MembershipExpiringRow;
import com.fitnesstraining.analytics.dto.OccupancyDayRow;
import com.fitnesstraining.analytics.dto.RevenueRow;
import com.fitnesstraining.checkin.model.CheckIn;
import com.fitnesstraining.checkin.repository.CheckInRepository;
import com.fitnesstraining.memberships.model.ClientMembership;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.payments.model.Payment;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.payments.repository.PaymentRepository;
import com.fitnesstraining.shared.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reportes de Analytics (RF-23 / RF-24), alineados al núcleo de Reportes del Club Deportivo
 * (vencimientos + mora + export) y ampliados con ingresos y ocupación.
 */
public class AnalyticsService {

    private final ClientMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final CheckInRepository checkInRepository;
    private final Clock clock;

    public AnalyticsService(
            ClientMembershipRepository membershipRepository,
            PaymentRepository paymentRepository,
            CheckInRepository checkInRepository,
            Clock clock) {
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.checkInRepository = checkInRepository;
        this.clock = clock;
    }

    public List<MembershipExpiringRow> listExpiringMemberships(int days) {
        if (days < 1 || days > 90) {
            throw new ValidationException("Los días deben estar entre 1 y 90.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime until = now.plusDays(days);
        return membershipRepository.listExpiringBetween(now, until).stream()
                .map(m -> toExpiringRow(m, now))
                .toList();
    }

    public List<DebtRow> listBlockingDebts() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return paymentRepository.listBlockingDebts(now).stream()
                .map(p -> toDebtRow(p, now))
                .toList();
    }

    public List<RevenueRow> listRevenue(LocalDate from, LocalDate toInclusive) {
        DateRange range = requireRange(from, toInclusive);
        return paymentRepository.listPaidBetween(range.from(), range.toExclusive()).stream()
                .map(this::toRevenueRow)
                .toList();
    }

    public List<OccupancyDayRow> listOccupancyByDay(LocalDate from, LocalDate toInclusive) {
        DateRange range = requireRange(from, toInclusive);
        List<CheckIn> checkIns = checkInRepository.listBetween(range.from(), range.toExclusive());

        Map<LocalDate, DayAgg> byDay = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(toInclusive); day = day.plusDays(1)) {
            byDay.put(day, new DayAgg());
        }
        for (CheckIn checkIn : checkIns) {
            LocalDate day = checkIn.getCheckedInAt().toLocalDate();
            DayAgg agg = byDay.get(day);
            if (agg == null) {
                continue;
            }
            agg.entries++;
            if (checkIn.getClient() != null && checkIn.getClient().getId() != null) {
                agg.clientIds.add(checkIn.getClient().getId());
            }
        }

        List<OccupancyDayRow> rows = new ArrayList<>(byDay.size());
        byDay.forEach((day, agg) -> rows.add(new OccupancyDayRow(day, agg.entries, agg.clientIds.size())));
        return rows;
    }

    private DateRange requireRange(LocalDate from, LocalDate toInclusive) {
        if (from == null || toInclusive == null) {
            throw new ValidationException("Indicá un rango de fechas completo.");
        }
        if (toInclusive.isBefore(from)) {
            throw new ValidationException("La fecha hasta no puede ser anterior a desde.");
        }
        if (ChronoUnit.DAYS.between(from, toInclusive) > 366) {
            throw new ValidationException("El rango máximo es de 366 días.");
        }
        ZoneId zone = clock.getZone();
        OffsetDateTime fromOd = from.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime toExclusive = toInclusive.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return new DateRange(fromOd, toExclusive);
    }

    private MembershipExpiringRow toExpiringRow(ClientMembership membership, OffsetDateTime now) {
        LocalDate endsOn = membership.getEndsAt().toLocalDate();
        int days = (int) ChronoUnit.DAYS.between(now.toLocalDate(), endsOn);
        days = Math.max(days, 0);
        return new MembershipExpiringRow(
                membership.getId(),
                membership.getClient().getId(),
                membership.getClient().getDocumentNumber(),
                membership.getClient().fullName(),
                membership.getPlan().getName(),
                membership.getPlan().getPrice(),
                endsOn,
                days,
                urgencyForExpiry(days));
    }

    private DebtRow toDebtRow(Payment payment, OffsetDateTime now) {
        LocalDate dueOn = payment.getDueAt() == null ? null : payment.getDueAt().toLocalDate();
        int daysOverdue = 0;
        if (dueOn != null) {
            daysOverdue = (int) Math.max(0, ChronoUnit.DAYS.between(dueOn, now.toLocalDate()));
        }
        return new DebtRow(
                payment.getId(),
                payment.getClient().getId(),
                payment.getClient().getDocumentNumber(),
                payment.getClient().fullName(),
                payment.getType(),
                payment.getAmount(),
                dueOn,
                daysOverdue,
                severityForDebt(daysOverdue, payment.getType()));
    }

    private RevenueRow toRevenueRow(Payment payment) {
        return new RevenueRow(
                payment.getId(),
                payment.getClient().getId(),
                payment.getClient().getDocumentNumber(),
                payment.getClient().fullName(),
                payment.getType(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getPaidAt().toLocalDate());
    }

    private static String urgencyForExpiry(int days) {
        if (days <= 3) {
            return "Crítico";
        }
        if (days <= 7) {
            return "Pronto";
        }
        return "A tiempo";
    }

    private static String severityForDebt(int daysOverdue, PaymentType type) {
        if (type == PaymentType.LATE_FEE || daysOverdue >= 7) {
            return "Alta";
        }
        if (daysOverdue >= 3) {
            return "Media";
        }
        return "Baja";
    }

    private record DateRange(OffsetDateTime from, OffsetDateTime toExclusive) {
    }

    private static final class DayAgg {
        private long entries;
        private final Set<Long> clientIds = new LinkedHashSet<>();
    }
}
