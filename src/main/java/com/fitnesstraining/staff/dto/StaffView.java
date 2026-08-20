package com.fitnesstraining.staff.dto;

import com.fitnesstraining.auth.model.RoleName;

import java.time.OffsetDateTime;

public record StaffView(
        Long id,
        String username,
        String displayName,
        String email,
        RoleName role,
        String roleLabel,
        boolean active,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {
}
