package com.fitnesstraining.staff.dto;

import com.fitnesstraining.auth.model.RoleName;

public record StaffRequest(
        String username,
        String displayName,
        String email,
        String password,
        RoleName role
) {
}
