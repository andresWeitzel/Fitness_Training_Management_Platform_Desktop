package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.WindowChrome;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class DemoAccountsController {

    @FXML private StackPane rootPane;

    private final AppContext appContext;

    public DemoAccountsController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        WindowChrome.fitStage(rootPane);
    }

    @FXML
    public void useAdmin() {
        appContext.returnToLoginWith("admin", "1234");
    }

    @FXML
    public void useReceptionist() {
        appContext.returnToLoginWith("empleado1", "emp123");
    }

    @FXML
    public void useTrainer() {
        appContext.returnToLoginWith("juan_prof", "prof123");
    }

    @FXML
    public void useNutritionist() {
        appContext.returnToLoginWith("maria_nutri", "nutri123");
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
