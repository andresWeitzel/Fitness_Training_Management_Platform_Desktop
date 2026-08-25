package com.fitnesstraining.training.dto;

public record RoutineItemRequest(
        Long exerciseId,
        Integer sets,
        String reps,
        Integer restSeconds,
        String loadNote,
        String notes
) {
}
