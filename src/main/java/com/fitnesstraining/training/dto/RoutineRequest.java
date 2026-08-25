package com.fitnesstraining.training.dto;

import com.fitnesstraining.training.model.RoutineStatus;

import java.time.LocalDate;
import java.util.List;

public record RoutineRequest(
        Long clientId,
        String title,
        String focus,
        String notes,
        RoutineStatus status,
        LocalDate startsOn,
        List<RoutineItemRequest> items
) {
}
