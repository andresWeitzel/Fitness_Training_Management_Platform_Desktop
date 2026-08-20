package com.fitnesstraining.staff.dto;

import com.fitnesstraining.auth.model.RoleName;

public record StaffSummary(
        Long id,
        String username,
        String displayName,
        String email,
        RoleName role,
        String roleLabel,
        boolean active
) {
}
