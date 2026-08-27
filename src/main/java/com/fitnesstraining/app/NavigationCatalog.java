package com.fitnesstraining.app;

import com.fitnesstraining.auth.model.PermissionCode;

import java.util.List;
import java.util.Optional;

public final class NavigationCatalog {

    private NavigationCatalog() {
    }

    public static List<NavItem> items() {
        return List.of(
                new NavItem("dashboard", "Panel", "Inicio", PermissionCode.DASHBOARD_VIEW,
                        "/views/dashboard.fxml", true, "Resumen del gimnasio y accesos rápidos."),
                new NavItem("clients", "Clientes", "Operación", PermissionCode.CLIENTS_VIEW,
                        "/views/clients.fxml", true, "Alta, ficha, baja y credenciales (n° cliente, carnet y QR)."),
                new NavItem("checkin", "Recepción", "Operación", PermissionCode.CHECKIN_MANAGE,
                        "/views/checkin.fxml", true, "Ingreso con carnet/QR, histórico y bloqueo por deuda."),
                new NavItem("memberships", "Membresías", "Comercial", PermissionCode.MEMBERSHIPS_MANAGE,
                        "/views/memberships.fxml", true, "Catálogo de planes y asignación a clientes."),
                new NavItem("payments", "Pagos", "Comercial", PermissionCode.PAYMENTS_MANAGE,
                        "/views/payments.fxml", true, "Cobros de membresía, mora/recargo e ingreso diario."),
                new NavItem("staff", "Personal", "Equipo", PermissionCode.STAFF_MANAGE,
                        "/views/staff.fxml", true, "ABM de usuarios internos y asignación de roles."),
                new NavItem("training", "Entrenamiento", "Entrenamiento", PermissionCode.TRAINING_MANAGE,
                        "/views/training.fxml", true, "Catálogo de ejercicios y rutinas estructuradas por cliente."),
                new NavItem("assessments", "Evaluaciones", "Seguimiento", PermissionCode.ASSESSMENTS_MANAGE,
                        "/views/assessments.fxml", true, "Historial de evaluaciones físicas por cliente."),
                new NavItem("nutrition", "Nutrición", "Seguimiento", PermissionCode.NUTRITION_MANAGE,
                        "/views/nutrition.fxml", true, "Turnos, planes nutricionales y ficha de salud con historial."),
                new NavItem("analytics", "Reportes", "Analytics", PermissionCode.ANALYTICS_VIEW,
                        "/views/analytics.fxml", true, "Vencimientos, mora, ingresos y ocupación con export CSV."),
                new NavItem("analytics-charts", "Gráficos", "Analytics", PermissionCode.ANALYTICS_VIEW,
                        "/views/analytics-charts.fxml", true, "Vista gráfica de indicadores operativos."),
                new NavItem("settings", "Base de datos", "Sistema", PermissionCode.SETTINGS_MANAGE,
                        "/views/database-settings.fxml", true, "Conexión PostgreSQL, migraciones y reconexión.")
        );
    }

    public static Optional<NavItem> byId(String id) {
        return items().stream().filter(item -> item.id().equals(id)).findFirst();
    }
}
