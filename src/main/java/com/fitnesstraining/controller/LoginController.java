package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.SceneNavigator;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.service.AuthService;
import com.fitnesstraining.shared.exception.AuthenticationException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button demoToggle;
    @FXML private VBox demoAccountsBox;

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
    public void onToggleDemoAccounts() {
        boolean show = !demoAccountsBox.isVisible();
        demoAccountsBox.setVisible(show);
        demoAccountsBox.setManaged(show);
        demoToggle.setText(show ? "Ocultar cuentas de prueba" : "Mostrar cuentas de prueba");
        Platform.runLater(() -> {
            Stage stage = (Stage) demoAccountsBox.getScene().getWindow();
            stage.sizeToScene();
        });
    }

    @FXML
    public void useAdmin() {
        fillDemo("admin", "1234");
    }

    @FXML
    public void useReceptionist() {
        fillDemo("empleado1", "emp123");
    }

    @FXML
    public void useTrainer() {
        fillDemo("juan_prof", "prof123");
    }

    @FXML
    public void useNutritionist() {
        fillDemo("maria_nutri", "nutri123");
    }

    private void fillDemo(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        visiblePasswordField.setText(password);
        errorLabel.setText("");
        loginButton.requestFocus();
    }

    @FXML
    public void onChangeDatabase() {
        appContext.openDatabaseSetup();
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
    }
}
