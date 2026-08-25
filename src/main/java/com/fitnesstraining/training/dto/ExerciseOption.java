package com.fitnesstraining.training.dto;

import com.fitnesstraining.training.model.EquipmentType;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;

public record ExerciseOption(
        Long id,
        String name,
        MuscleGroup muscleGroup,
        EquipmentType equipment,
        ExerciseDifficulty difficulty,
        String secondaryMuscles,
        String description,
        String techniqueNotes,
        String label
) {
}
