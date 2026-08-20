package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.NavItem;
import com.fitnesstraining.app.NavigationCatalog;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.dto.DashboardSnapshot;
import com.fitnesstraining.members.service.ClientQueryService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ADMIN", "Administrador",
            "RECEPTIONIST", "Recepción",
            "TRAINER", "Entrenador",
            "NUTRITIONIST", "Nutricionista"
    );

    @FXML private Label greetingLabel;
    @FXML private Label roleSummaryLabel;
    @FXML private Label activeClientsValue;
    @FXML private Label cardsValue;
    @FXML private Label qrValue;
    @FXML private Label inactiveValue;
    @FXML private Button openClientsButton;
    @FXML private Button newClientButton;
    @FXML private VBox recentList;
    @FXML private VBox modulesList;

    private final SessionContext sessionContext;
    private final ClientQueryService clientQueryService;
    private final AppContext appContext;

    public DashboardController(
            SessionContext sessionContext,
            ClientQueryService clientQueryService,
            AppContext appContext) {
        this.sessionContext = sessionContext;
        this.clientQueryService = clientQueryService;
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        String role = ROLE_LABELS.getOrDefault(user.primaryRole(), user.primaryRole());
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR")));
        greetingLabel.setText("Hola, " + user.displayName());
        roleSummaryLabel.setText(capitalize(today) + " · " + role);

        DashboardSnapshot snapshot = clientQueryService.loadSnapshot();
        activeClientsValue.setText(String.valueOf(snapshot.activeClients()));
        cardsValue.setText(String.valueOf(snapshot.activeCards()));
        qrValue.setText(String.valueOf(snapshot.activeQrCodes()));
        inactiveValue.setText(String.valueOf(snapshot.inactiveClients()));

        boolean canViewClients = user.hasPermission(com.fitnesstraining.auth.model.PermissionCode.CLIENTS_VIEW);
        openClientsButton.setVisible(canViewClients);
        openClientsButton.setManaged(canViewClients);
        newClientButton.setVisible(canViewClients);
        newClientButton.setManaged(canViewClients);

        fillRecent(snapshot);
        fillModules(user);
    }

    @FXML
    public void onOpenClients() {
        appContext.openModule("clients");
    }

    @FXML
    public void onOpenMemberships() {
        appContext.openModule("memberships");
    }

    @FXML
    public void onOpenPayments() {
        appContext.openModule("payments");
    }

    @FXML
    public void onOpenCheckin() {
        appContext.openModule("checkin");
    }

    private void fillRecent(DashboardSnapshot snapshot) {
        recentList.getChildren().clear();
        if (snapshot.recentClients().isEmpty()) {
            Label empty = new Label("Todavía no hay clientes cargados.");
            empty.getStyleClass().add("muted");
            recentList.getChildren().add(empty);
            return;
        }
        snapshot.recentClients().forEach(client -> recentList.getChildren().add(recentRow(client)));
    }

    private HBox recentRow(ClientSummary client) {
        Label name = new Label(client.fullName());
        name.getStyleClass().add("activity-name");
        Label meta = new Label(client.documentNumber() + " · " + (client.clientNumber().isBlank() ? "sin n°" : client.clientNumber()));
        meta.getStyleClass().add("muted");
        VBox text = new VBox(2, name, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label status = new Label(client.status() == com.fitnesstraining.members.model.ClientStatus.ACTIVE ? "Activo" : client.status().name());
        status.getStyleClass().add("badge-ready");
        HBox row = new HBox(10, text, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        return row;
    }

    private void fillModules(AuthenticatedUser user) {
        modulesList.getChildren().clear();
        var visible = NavigationCatalog.items().stream()
                .filter(item -> !"dashboard".equals(item.id()))
                .filter(item -> user.hasPermission(item.permission()))
                .toList();
        // Mostrar primero los listos y acotar la lista para no saturar el panel.
        visible.stream()
                .filter(NavItem::implemented)
                .forEach(item -> modulesList.getChildren().add(moduleRow(item)));
        visible.stream()
                .filter(item -> !item.implemented())
                .limit(4)
                .forEach(item -> modulesList.getChildren().add(moduleRow(item)));
        long remaining = visible.stream().filter(item -> !item.implemented()).count() - 4;
        if (remaining > 0) {
            Label more = new Label("+" + remaining + " módulos próximos en el menú lateral");
            more.getStyleClass().add("muted");
            modulesList.getChildren().add(more);
        }
    }

    private HBox moduleRow(NavItem item) {
        Label name = new Label(item.label());
        name.getStyleClass().add("activity-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label(item.implemented() ? "Listo" : "Próximo");
        badge.getStyleClass().add(item.implemented() ? "badge-ready" : "badge-soon");
        HBox row = new HBox(10, name, spacer, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        row.setOnMouseClicked(event -> appContext.openModule(item.id()));
        return row;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
