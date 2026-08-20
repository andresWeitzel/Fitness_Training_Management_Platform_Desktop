package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.WindowChrome;
import com.fitnesstraining.auth.service.DemoCredentialStore;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class DemoAccountsController {

    @FXML private StackPane rootPane;
    @FXML private VBox accountsBox;
    @FXML private Label emptyHintLabel;

    private final AppContext appContext;

    public DemoAccountsController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        renderAccounts();
        WindowChrome.fitStage(rootPane);
    }

    private void renderAccounts() {
        accountsBox.getChildren().clear();
        List<DemoCredentialStore.DemoAccount> accounts = appContext.demoPanelAccounts();
        boolean noneAssignable = accounts.stream().noneMatch(a -> a.userId() != null);
        emptyHintLabel.setVisible(noneAssignable);
        emptyHintLabel.setManaged(noneAssignable);

        for (DemoCredentialStore.DemoAccount account : accounts) {
            Button button = new Button(DemoCredentialStore.labelFor(account));
            button.getStyleClass().add("demo-pick");
            button.setMaxWidth(Double.MAX_VALUE);
            if (account.userId() != null && account.passwordKnown()) {
                button.setOnAction(e ->
                        appContext.returnToLoginWith(account.username(), account.password()));
            } else if (account.userId() != null) {
                button.setOnAction(e ->
                        appContext.returnToLoginWith(account.username(), ""));
            } else {
                button.setDisable(true);
            }
            accountsBox.getChildren().add(button);
        }
    }

    @FXML
    public void onBackToLogin() {
        appContext.returnToLogin();
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
    }
}
