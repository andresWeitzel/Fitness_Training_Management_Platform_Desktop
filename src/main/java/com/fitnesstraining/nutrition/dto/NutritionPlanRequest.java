package com.fitnesstraining.nutrition.dto;

import com.fitnesstraining.nutrition.model.NutritionPlanStatus;

import java.time.LocalDate;

public record NutritionPlanRequest(
        Long clientId,
        String title,
        String objectives,
        String mealGuidance,
        NutritionPlanStatus status,
        LocalDate validFrom,
        LocalDate validUntil,
        String notes
) {
}
