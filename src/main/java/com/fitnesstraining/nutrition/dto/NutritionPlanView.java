package com.fitnesstraining.nutrition.dto;

import com.fitnesstraining.nutrition.model.NutritionPlanStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record NutritionPlanView(
        Long id,
        Long clientId,
        String clientName,
        String clientDocument,
        String clientNumber,
        Long createdByUserId,
        String createdByName,
        String title,
        String objectives,
        String mealGuidance,
        NutritionPlanStatus status,
        LocalDate validFrom,
        LocalDate validUntil,
        String notes,
        OffsetDateTime createdAt
) {
}
