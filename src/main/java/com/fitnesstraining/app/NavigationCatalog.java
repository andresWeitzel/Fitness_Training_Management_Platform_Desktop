package com.fitnesstraining.app;

import com.fitnesstraining.auth.model.PermissionCode;

import java.util.List;

public final class NavigationCatalog {

    private NavigationCatalog() {
    }

    public static List<NavItem> items() {
        return List.of(
                new NavItem("dashboard", "Panel", "Inicio", PermissionCode.DASHBOARD_VIEW, "/views/dashboard.fxml"),
                new NavItem("clients", "Clientes", "Gestión", PermissionCode.CLIENTS_VIEW, "/views/clients.fxml"),
                new NavItem("memberships", "Membresías", "Gestión", PermissionCode.MEMBERSHIPS_MANAGE, "/views/placeholder.fxml"),
                new NavItem("payments", "Pagos", "Gestión", PermissionCode.PAYMENTS_MANAGE, "/views/placeholder.fxml"),
                new NavItem("checkin", "Recepción", "Gestión", PermissionCode.CHECKIN_MANAGE, "/views/placeholder.fxml"),
                new NavItem("staff", "Personal", "Gestión", PermissionCode.STAFF_MANAGE, "/views/placeholder.fxml"),
                new NavItem("training", "Entrenamiento", "Entrenamiento", PermissionCode.TRAINING_MANAGE, "/views/placeholder.fxml"),
                new NavItem("assessments", "Evaluaciones", "Seguimiento", PermissionCode.ASSESSMENTS_MANAGE, "/views/placeholder.fxml"),
                new NavItem("nutrition", "Nutrición", "Seguimiento", PermissionCode.NUTRITION_MANAGE, "/views/placeholder.fxml"),
                new NavItem("analytics", "Analytics", "Analytics", PermissionCode.ANALYTICS_VIEW, "/views/placeholder.fxml")
        );
    }
}
