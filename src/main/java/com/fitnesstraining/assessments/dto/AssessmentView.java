package com.fitnesstraining.assessments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AssessmentView(
        Long id,
        Long clientId,
        String clientDocument,
        String clientName,
        String clientNumber,
        Long assessorUserId,
        String assessorName,
        OffsetDateTime assessedAt,
        BigDecimal weightKg,
        BigDecimal heightCm,
        BigDecimal bmi,
        BigDecimal bodyFatPct,
        BigDecimal waistCm,
        BigDecimal hipCm,
        BigDecimal chestCm,
        String notes
) {
}
