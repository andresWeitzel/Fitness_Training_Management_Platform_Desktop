package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.NavItem;
import com.fitnesstraining.app.NavigationCatalog;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.checkin.dto.CheckInSummary;
import com.fitnesstraining.checkin.service.CheckInService;
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
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ADMIN", "Administrador",
            "RECEPTIONIST", "Recepción",
            "TRAINER", "Entrenador",
            "NUTRITIONIST", "Nutricionista"
    );
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label greetingLabel;
    @FXML private Label roleSummaryLabel;
    @FXML private Label activeClientsValue;
    @FXML private Label cardsValue;
    @FXML private Label qrValue;
    @FXML private Label inactiveValue;
    @FXML private Button openClientsButton;
    @FXML private Button newClientButton;
    @FXML private Label recentCountLabel;
    @FXML private Label recentPreviewLabel;
    @FXML private VBox recentTeaserBox;
    @FXML private Label todayCheckInCountLabel;
    @FXML private Label todayCheckInPreviewLabel;
    @FXML private VBox todayTeaserBox;
    @FXML private Label modulesCountLabel;
    @FXML private Label modulesPreviewLabel;
    @FXML private VBox modulesTeaserBox;
    @FXML private VBox recentActivityCard;
    @FXML private VBox todayCheckInCard;
    @FXML private VBox modulesStatusCard;
    @FXML private VBox quickAccessCard;

    private final SessionContext sessionContext;
    private final ClientQueryService clientQueryService;
    private final CheckInService checkInService;
    private final AppContext appContext;

    private List<ClientSummary> recentClients = List.of();
    private List<CheckInSummary> todayCheckIns = List.of();
    private List<NavItem> visibleModules = List.of();
    private boolean canViewClients;
    private boolean canManageCheckIn;

    public DashboardController(
            SessionContext sessionContext,
            ClientQueryService clientQueryService,
            CheckInService checkInService,
            AppContext appContext) {
        this.sessionContext = sessionContext;
        this.clientQueryService = clientQueryService;
        this.checkInService = checkInService;
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

        canViewClients = user.hasPermission(PermissionCode.CLIENTS_VIEW);
        canManageCheckIn = user.hasPermission(PermissionCode.CHECKIN_MANAGE);
        openClientsButton.setVisible(canViewClients);
        openClientsButton.setManaged(canViewClients);
        newClientButton.setVisible(canViewClients);
        newClientButton.setManaged(canViewClients);
        todayCheckInCard.setDisable(!canManageCheckIn);
        todayCheckInCard.setOpacity(canManageCheckIn ? 1.0 : 0.72);

        recentClients = clientQueryService.listRecentClients(20);
        todayCheckIns = canManageCheckIn ? checkInService.listToday() : List.of();
        visibleModules = NavigationCatalog.items().stream()
                .filter(item -> !"dashboard".equals(item.id()))
                .filter(item -> user.hasPermission(item.permission()))
                .toList();

        fillRecentPreview();
        fillTodayPreview();
        fillModulesPreview();
        stylePanelCards();
    }

    private void stylePanelCards() {
        String cardCss = """
                -fx-background-color: #FFFFFF;
                -fx-background-radius: 20;
                -fx-border-radius: 20;
                -fx-border-color: #D7E0EA;
                -fx-border-width: 1;
                """;
        if (recentActivityCard != null) {
            recentActivityCard.setStyle(cardCss);
        }
        if (todayCheckInCard != null) {
            todayCheckInCard.setStyle(cardCss);
        }
        if (modulesStatusCard != null) {
            modulesStatusCard.setStyle(cardCss);
        }
        if (quickAccessCard != null) {
            quickAccessCard.setStyle(cardCss);
        }
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

    @FXML
    public void onOpenRecentActivity() {
        DashboardRecentActivityController.open(
                ownerWindow(),
                recentClients,
                appContext,
                canViewClients,
                this::onOpenClients);
    }

    @FXML
    public void onOpenTodayCheckIns() {
        if (!canManageCheckIn) {
            return;
        }
        DashboardTodayCheckInsController.open(
                ownerWindow(),
                todayCheckIns,
                appContext,
                canManageCheckIn,
                this::onOpenCheckin);
    }

    @FXML
    public void onOpenModulesStatus() {
        DashboardModulesController.open(ownerWindow(), visibleModules, appContext);
    }

    private void fillRecentPreview() {
        recentTeaserBox.getChildren().clear();
        int total = recentClients.size();
        recentCountLabel.setText(String.valueOf(total));
        if (total == 0) {
            recentPreviewLabel.setText("Todavía no hay altas");
            return;
        }
        recentPreviewLabel.setText(total + (total == 1 ? " alta reciente" : " altas recientes"));
        recentClients.stream().limit(3).forEach(client ->
                recentTeaserBox.getChildren().add(teaserRow(
                        client.fullName(),
                        client.documentNumber()
                                + " · "
                                + (client.clientNumber() == null || client.clientNumber().isBlank()
                                ? "sin n°"
                                : "N° " + client.clientNumber()),
                        client.status() == com.fitnesstraining.members.model.ClientStatus.ACTIVE ? "Activo" : "Baja",
                        client.status() == com.fitnesstraining.members.model.ClientStatus.ACTIVE)));
    }

    private void fillTodayPreview() {
        todayTeaserBox.getChildren().clear();
        if (!canManageCheckIn) {
            todayCheckInCountLabel.setText("—");
            todayCheckInPreviewLabel.setText("Sin permiso de recepción");
            return;
        }
        int total = todayCheckIns.size();
        todayCheckInCountLabel.setText(String.valueOf(total));
        if (total == 0) {
            todayCheckInPreviewLabel.setText("Sin ingresos todavía hoy");
            return;
        }
        todayCheckInPreviewLabel.setText(total + (total == 1 ? " ingreso hoy" : " ingresos hoy"));
        todayCheckIns.stream().limit(3).forEach(row ->
                todayTeaserBox.getChildren().add(teaserRow(
                        row.clientName(),
                        row.clientDocument() + " · " + TIME_FORMAT.format(row.checkedInAt().toLocalTime()),
                        "Ingreso",
                        true)));
    }

    private void fillModulesPreview() {
        modulesTeaserBox.getChildren().clear();
        long ready = visibleModules.stream().filter(NavItem::implemented).count();
        long upcoming = visibleModules.size() - ready;
        modulesCountLabel.setText(String.valueOf(ready));
        modulesPreviewLabel.setText(ready + " listos · " + upcoming + " próximos");
        visibleModules.stream()
                .filter(NavItem::implemented)
                .limit(3)
                .forEach(item -> modulesTeaserBox.getChildren().add(teaserRow(
                        item.label(),
                        item.group(),
                        "Listo",
                        true)));
        if (upcoming > 0) {
            visibleModules.stream()
                    .filter(item -> !item.implemented())
                    .findFirst()
                    .ifPresent(item -> modulesTeaserBox.getChildren().add(teaserRow(
                            item.label(),
                            item.group(),
                            "Próximo",
                            false)));
        }
    }

    private HBox teaserRow(String title, String meta, String badgeText, boolean ready) {
        Label name = new Label(title);
        name.getStyleClass().add("activity-name");
        Label detail = new Label(meta);
        detail.getStyleClass().add("muted");
        VBox text = new VBox(1, name, detail);
        HBox.setHgrow(text, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label(badgeText);
        badge.getStyleClass().add(ready ? "badge-ready" : "badge-soon");
        HBox row = new HBox(8, text, spacer, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dash-teaser-row");
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Window ownerWindow() {
        if (recentActivityCard != null && recentActivityCard.getScene() != null) {
            return recentActivityCard.getScene().getWindow();
        }
        if (modulesStatusCard != null && modulesStatusCard.getScene() != null) {
            return modulesStatusCard.getScene().getWindow();
        }
        return null;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
