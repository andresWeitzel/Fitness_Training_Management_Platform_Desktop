package com.fitnesstraining.nutrition.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record NutritionAppointmentRequest(
        Long clientId,
        LocalDate scheduledOn,
        LocalTime scheduledTime,
        String notes
) {
}
