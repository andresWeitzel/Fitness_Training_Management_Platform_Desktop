package com.fitnesstraining.nutrition.dto;

import com.fitnesstraining.nutrition.model.NutritionPlanStatus;

import java.time.LocalDate;

public record NutritionPlanSummary(
        Long id,
        Long clientId,
        String clientName,
        String title,
        NutritionPlanStatus status,
        LocalDate validFrom,
        LocalDate validUntil,
        String authorName
) {
}
