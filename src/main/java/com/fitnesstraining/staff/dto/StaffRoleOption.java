package com.fitnesstraining.staff.dto;

import com.fitnesstraining.auth.model.RoleName;

public record StaffRoleOption(
        RoleName role,
        String label,
        String description
) {
}
