package com.fitnesstraining.analytics.dto;

import java.time.LocalDate;

public record OccupancyDayRow(
        LocalDate day,
        long entries,
        long uniqueClients
) {
}
