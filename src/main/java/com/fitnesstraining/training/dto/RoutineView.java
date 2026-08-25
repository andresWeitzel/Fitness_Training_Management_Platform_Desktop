package com.fitnesstraining.training.dto;

import com.fitnesstraining.training.model.EquipmentType;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;
import com.fitnesstraining.training.model.RoutineStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RoutineView(
        Long id,
        Long clientId,
        String clientName,
        String clientDocument,
        Long trainerUserId,
        String trainerName,
        String title,
        String focus,
        String notes,
        RoutineStatus status,
        String statusLabel,
        LocalDate startsOn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RoutineItemView> items
) {
    public record RoutineItemView(
            Long id,
            Long exerciseId,
            String exerciseName,
            MuscleGroup muscleGroup,
            String muscleGroupLabel,
            EquipmentType equipment,
            String equipmentLabel,
            ExerciseDifficulty difficulty,
            String difficultyLabel,
            String secondaryMuscles,
            String description,
            String techniqueNotes,
            Integer sets,
            String reps,
            Integer restSeconds,
            String loadNote,
            String notes,
            int sortOrder
    ) {
    }
}
