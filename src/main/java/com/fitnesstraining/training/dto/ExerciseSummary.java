package com.fitnesstraining.training.dto;

import com.fitnesstraining.training.model.EquipmentType;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;

public record ExerciseSummary(
        Long id,
        String name,
        MuscleGroup muscleGroup,
        String muscleGroupLabel,
        EquipmentType equipment,
        String equipmentLabel,
        ExerciseDifficulty difficulty,
        String difficultyLabel,
        String secondaryMuscles,
        String description,
        String techniqueNotes,
        boolean active
) {
}
