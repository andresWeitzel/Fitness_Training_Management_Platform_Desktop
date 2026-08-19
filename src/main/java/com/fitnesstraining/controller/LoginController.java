package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.SceneNavigator;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.service.AuthService;
import com.fitnesstraining.shared.exception.AuthenticationException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final AuthService authService;
    private final SessionContext sessionContext;
    private final SceneNavigator navigator;
    private final AppContext appContext;

    public LoginController(
            AuthService authService,
            SessionContext sessionContext,
            SceneNavigator navigator,
            AppContext appContext) {
        this.authService = authService;
        this.sessionContext = sessionContext;
        this.navigator = navigator;
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        errorLabel.setText("");
        usernameField.setText("admin");
        passwordField.setOnAction(event -> onLogin());
        visiblePasswordField.setOnAction(event -> onLogin());
        usernameField.setOnAction(event -> passwordField.requestFocus());
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        showPasswordCheck.selectedProperty().addListener((obs, was, show) -> togglePasswordVisible(show));
    }

    @FXML
    public void onLogin() {
        errorLabel.setText("");
        loginButton.setDisable(true);
        try {
            AuthenticatedUser user = authService.login(usernameField.getText(), currentPassword());
            sessionContext.start(user);
            navigator.showShell();
        } catch (AuthenticationException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("No se pudo iniciar sesión: " + ex.getMessage());
        } finally {
            loginButton.setDisable(false);
        }
    }

    private String currentPassword() {
        return showPasswordCheck.isSelected() ? visiblePasswordField.getText() : passwordField.getText();
    }

    private void togglePasswordVisible(boolean show) {
        visiblePasswordField.setManaged(show);
        visiblePasswordField.setVisible(show);
        passwordField.setManaged(!show);
        passwordField.setVisible(!show);
        if (show) {
            visiblePasswordField.requestFocus();
            visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    public void onChangeDatabase() {
        appContext.openDatabaseSetup();
    }
}
