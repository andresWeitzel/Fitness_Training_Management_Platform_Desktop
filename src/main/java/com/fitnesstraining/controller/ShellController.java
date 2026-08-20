package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.DbSetupMode;
import com.fitnesstraining.app.NavItem;
import com.fitnesstraining.app.NavigationCatalog;
import com.fitnesstraining.app.SceneNavigator;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.app.WindowChrome;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.service.AuthorizationService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShellController {

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ADMIN", "Administrador",
            "RECEPTIONIST", "Recepción",
            "TRAINER", "Entrenador",
            "NUTRITIONIST", "Nutricionista"
    );

    private static final Map<String, String> NAV_ICONS = Map.ofEntries(
            Map.entry("dashboard", "Pn"),
            Map.entry("clients", "Cl"),
            Map.entry("memberships", "Mb"),
            Map.entry("payments", "Pg"),
            Map.entry("checkin", "Rc"),
            Map.entry("staff", "Ps"),
            Map.entry("training", "En"),
            Map.entry("assessments", "Ev"),
            Map.entry("nutrition", "Nu"),
            Map.entry("analytics", "An"),
            Map.entry("settings", "Db")
    );

    @FXML private VBox navContainer;
    @FXML private Label userLabel;
    @FXML private Label roleLabel;
    @FXML private Label sidebarUserLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label pageTitle;
    @FXML private StackPane contentHost;
    @FXML private ScrollPane contentScroll;

    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;
    private final SceneNavigator navigator;
    private final AppContext appContext;

    public ShellController(
            SessionContext sessionContext,
            AuthorizationService authorizationService,
            SceneNavigator navigator,
            AppContext appContext) {
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
        this.navigator = navigator;
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        appContext.registerShell(this);
        AuthenticatedUser user = sessionContext.requireUser();
        String role = ROLE_LABELS.getOrDefault(user.primaryRole(), user.primaryRole());
        userLabel.setText(user.displayName());
        roleLabel.setText(role);
        sidebarUserLabel.setText(user.displayName());
        sidebarRoleLabel.setText(role);
        buildNavigation(user);
    }

    public void showHome() {
        open(NavigationCatalog.items().getFirst());
    }

    public void openById(String id) {
        NavigationCatalog.byId(id)
                .filter(item -> authorizationService.hasPermission(sessionContext.requireUser(), item.permission()))
                .ifPresent(this::open);
    }

    @FXML
    public void onLogout() {
        appContext.logout();
    }

    @FXML
    public void onMinimize() {
        appContext.stage().setIconified(true);
    }

    @FXML
    public void onToggleMaximize() {
        var stage = appContext.stage();
        var root = stage.getScene().getRoot();
        if (!stage.isMaximized()) {
            WindowChrome.rememberShellBounds(stage);
        }
        boolean maximize = !stage.isMaximized();
        stage.setMaximized(maximize);
        root.getStyleClass().remove("maximized");
        if (maximize) {
            root.getStyleClass().add("maximized");
        }
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
    }

    private void buildNavigation(AuthenticatedUser user) {
        navContainer.getChildren().clear();
        Map<String, List<NavItem>> grouped = new LinkedHashMap<>();
        NavigationCatalog.items().stream()
                .filter(item -> authorizationService.hasPermission(user, item.permission()))
                .forEach(item -> grouped.computeIfAbsent(item.group(), key -> new java.util.ArrayList<>()).add(item));

        grouped.forEach((group, items) -> {
            Label groupLabel = new Label(group.toUpperCase());
            groupLabel.getStyleClass().add("nav-group");
            navContainer.getChildren().add(groupLabel);

            VBox groupBox = new VBox(4);
            groupBox.getStyleClass().add("nav-group-box");
            items.forEach(item -> groupBox.getChildren().add(navButton(item)));
            navContainer.getChildren().add(groupBox);
        });
    }

    private Button navButton(NavItem item) {
        Label icon = new Label(NAV_ICONS.getOrDefault(item.id(), "•"));
        icon.getStyleClass().add("nav-icon");

        Label title = new Label(item.label());
        title.getStyleClass().add("nav-label");
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox content = new HBox(10, icon, title);
        content.setAlignment(Pos.CENTER_LEFT);
        if (!item.implemented()) {
            Label soon = new Label("Pronto");
            soon.getStyleClass().add("nav-soon-badge");
            content.getChildren().add(soon);
        }

        Button button = new Button();
        button.setGraphic(content);
        button.getStyleClass().add("nav-button");
        if (!item.implemented()) {
            button.getStyleClass().add("nav-soon");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setUserData(item.id());
        button.setOnAction(event -> open(item));
        return button;
    }

    private void open(NavItem item) {
        pageTitle.setText(item.label());
        highlightNav(item);
        if (contentScroll != null) {
            contentScroll.setVvalue(0);
        }
        if (!item.implemented()) {
            contentHost.getChildren().setAll(navigator.loadPlaceholder(item.label(), item.summary()));
            return;
        }
        var loaded = navigator.views().load(item.fxml());
        if (loaded.controller() instanceof DbSetupController dbSetup) {
            dbSetup.prepare(DbSetupMode.ADMIN, null);
        }
        contentHost.getChildren().setAll(loaded.root());
    }

    private void highlightNav(NavItem item) {
        navContainer.lookupAll(".nav-button").forEach(node -> {
            node.getStyleClass().remove("selected");
            if (node instanceof Button button && item.id().equals(String.valueOf(button.getUserData()))) {
                button.getStyleClass().add("selected");
            }
        });
    }
}
