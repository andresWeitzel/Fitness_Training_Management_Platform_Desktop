package com.fitnesstraining.nutrition.dto;

import com.fitnesstraining.nutrition.model.NutritionAppointmentStatus;

import java.time.OffsetDateTime;

public record NutritionAppointmentView(
        Long id,
        Long clientId,
        String clientName,
        String clientDocument,
        String clientNumber,
        Long nutritionistUserId,
        String nutritionistName,
        OffsetDateTime scheduledAt,
        NutritionAppointmentStatus status,
        String notes
) {
}
