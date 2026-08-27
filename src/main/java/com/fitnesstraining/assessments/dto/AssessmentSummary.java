package com.fitnesstraining.assessments.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AssessmentSummary(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        OffsetDateTime assessedAt,
        BigDecimal weightKg,
        BigDecimal heightCm,
        BigDecimal bmi,
        BigDecimal bodyFatPct,
        String assessorName
) {
}
