package com.fitnesstraining.training.dto;

import com.fitnesstraining.training.model.RoutineStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RoutineSummary(
        Long id,
        Long clientId,
        String clientName,
        String clientDocument,
        String title,
        String focus,
        int itemCount,
        RoutineStatus status,
        String statusLabel,
        String trainerName,
        LocalDate startsOn,
        OffsetDateTime updatedAt
) {
}
