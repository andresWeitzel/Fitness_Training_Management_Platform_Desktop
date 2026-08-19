package com.fitnesstraining.controller;

import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.members.service.ClientQueryService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label greetingLabel;
    @FXML private Label roleSummaryLabel;
    @FXML private Label activeClientsValue;
    @FXML private Label presentValue;
    @FXML private Label membershipsValue;
    @FXML private Label revenueValue;

    private final SessionContext sessionContext;
    private final ClientQueryService clientQueryService;

    public DashboardController(SessionContext sessionContext, ClientQueryService clientQueryService) {
        this.sessionContext = sessionContext;
        this.clientQueryService = clientQueryService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        greetingLabel.setText("Hola, " + user.displayName());
        roleSummaryLabel.setText("Sesión como " + user.primaryRole() + " · " + user.permissions().size() + " permisos");
        activeClientsValue.setText(String.valueOf(clientQueryService.countActiveClients()));
        presentValue.setText("—");
        membershipsValue.setText("—");
        revenueValue.setText("—");
    }
}
