package com.fitnesstraining.nutrition.dto;

import com.fitnesstraining.nutrition.model.NutritionAppointmentStatus;

import java.time.OffsetDateTime;

public record NutritionAppointmentSummary(
        Long id,
        Long clientId,
        String clientName,
        String clientDocument,
        OffsetDateTime scheduledAt,
        NutritionAppointmentStatus status,
        String nutritionistName
) {
}
