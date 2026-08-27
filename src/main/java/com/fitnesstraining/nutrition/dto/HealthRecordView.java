package com.fitnesstraining.nutrition.dto;

import java.time.OffsetDateTime;

public record HealthRecordView(
        Long id,
        Long clientId,
        String clientName,
        String clientDocument,
        String clientNumber,
        Long recordedByUserId,
        String recordedByName,
        OffsetDateTime recordedAt,
        String allergies,
        String restrictions,
        String conditions,
        String medications,
        String notes
) {
}
