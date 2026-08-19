package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.NavItem;
import com.fitnesstraining.app.NavigationCatalog;
import com.fitnesstraining.app.SceneNavigator;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.service.AuthorizationService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShellController {

    @FXML private VBox navContainer;
    @FXML private Label userLabel;
    @FXML private Label roleLabel;
    @FXML private Label pageTitle;
    @FXML private StackPane contentHost;

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
        AuthenticatedUser user = sessionContext.requireUser();
        userLabel.setText(user.displayName());
        roleLabel.setText(user.primaryRole());
        buildNavigation(user);
    }

    public void showHome() {
        open(NavigationCatalog.items().getFirst());
    }

    @FXML
    public void onLogout() {
        appContext.logout();
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
            items.forEach(item -> navContainer.getChildren().add(navButton(item)));
        });
    }

    private Button navButton(NavItem item) {
        Button button = new Button(item.label());
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> open(item));
        return button;
    }

    private void open(NavItem item) {
        pageTitle.setText(item.label());
        highlightNav(item);
        if ("/views/placeholder.fxml".equals(item.fxml())) {
            contentHost.getChildren().setAll(navigator.loadPlaceholder(
                    item.label(),
                    "Este módulo se implementará en una fase posterior. La navegación ya respeta los permisos de "
                            + sessionContext.requireUser().primaryRole() + "."
            ));
            return;
        }
        contentHost.getChildren().setAll(navigator.views().load(item.fxml()).root());
    }

    private void highlightNav(NavItem item) {
        navContainer.lookupAll(".nav-button").forEach(node -> {
            node.getStyleClass().remove("selected");
            if (node instanceof Button button && item.label().equals(button.getText())) {
                button.getStyleClass().add("selected");
            }
        });
    }
}
