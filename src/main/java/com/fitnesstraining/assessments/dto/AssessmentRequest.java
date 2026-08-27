package com.fitnesstraining.assessments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssessmentRequest(
        Long clientId,
        LocalDate assessedOn,
        BigDecimal weightKg,
        BigDecimal heightCm,
        BigDecimal bodyFatPct,
        BigDecimal waistCm,
        BigDecimal hipCm,
        BigDecimal chestCm,
        String notes
) {
}
