package com.fitnesstraining.nutrition.dto;

import java.time.OffsetDateTime;

public record HealthRecordSummary(
        Long id,
        Long clientId,
        String clientName,
        OffsetDateTime recordedAt,
        String allergiesPreview,
        String restrictionsPreview,
        String authorName
) {
}
