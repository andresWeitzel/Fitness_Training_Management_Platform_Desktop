package com.fitnesstraining.auth.dto;

import com.fitnesstraining.auth.model.Permission;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.User;

import java.util.Set;
import java.util.stream.Collectors;

public record AuthenticatedUser(
        Long id,
        String username,
        String displayName,
        Set<String> roles,
        Set<String> permissions
) {

    public static AuthenticatedUser from(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> permissionCodes = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                roleNames,
                permissionCodes
        );
    }

    public boolean hasPermission(PermissionCode code) {
        return permissions.contains(code.name());
    }

    public boolean hasAnyPermission(PermissionCode... codes) {
        for (PermissionCode code : codes) {
            if (hasPermission(code)) {
                return true;
            }
        }
        return false;
    }

    public String primaryRole() {
        return roles.stream().sorted().findFirst().orElse("SIN_ROL");
    }
}
