package com.fitnesstraining.nutrition.dto;

import java.time.LocalDate;

public record HealthRecordRequest(
        Long clientId,
        LocalDate recordedOn,
        String allergies,
        String restrictions,
        String conditions,
        String medications,
        String notes
) {
}
