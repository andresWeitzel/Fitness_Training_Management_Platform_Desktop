package com.fitnesstraining.app;

import com.fitnesstraining.auth.model.PermissionCode;

public record NavItem(
        String id,
        String label,
        String group,
        PermissionCode permission,
        String fxml
) {
}
