package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.DbConnectionSnapshot;
import com.fitnesstraining.app.WindowChrome;
import com.fitnesstraining.app.SceneNavigator;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.dto.PendingLoginFill;
import com.fitnesstraining.auth.service.AuthService;
import com.fitnesstraining.shared.config.DatabaseBootstrap;
import com.fitnesstraining.shared.config.DatabaseConfigStore;
import com.fitnesstraining.shared.config.DatabaseSettings;
import com.fitnesstraining.shared.exception.AuthenticationException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private HBox connectionBar;
    @FXML private Label connectionStatusLabel;
    @FXML private ProgressIndicator connectionSpinner;
    @FXML private Button verifyConnectionButton;
    @FXML private StackPane rootPane;

    private boolean busy;

    private final SessionContext sessionContext;
    private final SceneNavigator navigator;
    private final AppContext appContext;
    private final DatabaseConfigStore configStore;
    private final DatabaseBootstrap databaseBootstrap;

    public LoginController(
            AuthService authService,
            SessionContext sessionContext,
            SceneNavigator navigator,
            AppContext appContext,
            DatabaseConfigStore configStore,
            DatabaseBootstrap databaseBootstrap) {
        this.sessionContext = sessionContext;
        this.navigator = navigator;
        this.appContext = appContext;
        this.configStore = configStore;
        this.databaseBootstrap = databaseBootstrap;
    }

    @FXML
    public void initialize() {
        errorLabel.setText("");
        passwordField.setOnAction(event -> onLogin());
        visiblePasswordField.setOnAction(event -> onLogin());
        usernameField.setOnAction(event -> passwordField.requestFocus());
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        showPasswordCheck.selectedProperty().addListener((obs, was, show) -> togglePasswordVisible(show));
        errorLabel.textProperty().addListener((obs, old, text) -> {
            boolean hasError = text != null && !text.isBlank();
            errorLabel.setManaged(hasError);
            errorLabel.setVisible(hasError);
            fitWindow();
        });
        appContext.consumePendingLogin().ifPresent(this::applyFill);
        // Descarta el detalle técnico del arranque; el usuario solo ve el indicador.
        appContext.consumePendingConnectionError();
        if (!appContext.isDatabaseReady()) {
            showConnectionError();
        } else {
            restoreConnectionSnapshot();
        }
        fitWindow();
    }

    private void fitWindow() {
        WindowChrome.fitStage(rootPane);
    }

    @FXML
    public void onLogin() {
        if (busy) {
            return;
        }
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = currentPassword();

        if (appContext.isDatabaseReady()) {
            attemptLogin(username, password);
            return;
        }

        configStore.load().ifPresentOrElse(
                settings -> prepareConnectionThen(settings, () -> attemptLogin(username, password)),
                this::showConnectionError);
    }

    @FXML
    public void onVerifyConnection() {
        if (busy) {
            return;
        }
        configStore.load().ifPresentOrElse(
                settings -> prepareConnectionThen(settings, null),
                this::showConnectionError);
    }

    private void prepareConnectionThen(DatabaseSettings settings, Runnable afterReady) {
        setBusy(true);
        showConnectionPending();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                databaseBootstrap.testConnection(settings);
                return null;
            }
        };
        task.setOnSucceeded(event -> Platform.runLater(() -> {
            try {
                if (!appContext.isDatabaseReady()) {
                    appContext.ensureDatabaseReady(settings);
                }
                showConnectionOk(settings);
                if (afterReady != null) {
                    afterReady.run();
                }
            } catch (Exception ex) {
                showConnectionError();
            } finally {
                setBusy(false);
                fitWindow();
            }
        }));
        task.setOnFailed(event -> Platform.runLater(() -> {
            showConnectionError();
            setBusy(false);
            fitWindow();
        }));
        new Thread(task, "login-connection").start();
    }

    private void attemptLogin(String username, String password) {
        AuthService authService = appContext.authService().orElse(null);
        if (authService == null) {
            showConnectionError();
            return;
        }
        setBusy(true);
        try {
            AuthenticatedUser user = authService.login(username, password);
            sessionContext.start(user);
            navigator.showShell();
        } catch (AuthenticationException ex) {
            errorLabel.setText(ex.getMessage());
            setBusy(false);
        } catch (Exception ex) {
            errorLabel.setText("No se pudo iniciar sesión.");
            setBusy(false);
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        loginButton.setDisable(value);
        verifyConnectionButton.setDisable(value);
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
    public void onOpenDemoAccounts() {
        appContext.openDemoAccounts();
    }

    private void applyFill(PendingLoginFill fill) {
        usernameField.setText(fill.username());
        passwordField.setText(fill.password());
        visiblePasswordField.setText(fill.password());
        errorLabel.setText("");
        usernameField.requestFocus();
        fitWindow();
    }

    private void restoreConnectionSnapshot() {
        DbConnectionSnapshot snapshot = appContext.connectionSnapshot();
        switch (snapshot.status()) {
            case CONNECTED -> applyConnectionState("db-status-ok", "Conectada");
            case DISCONNECTED -> applyConnectionState("db-status-error", "Sin conexión");
            default -> showConnectionIdle();
        }
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText(snapshot.status() == DbConnectionSnapshot.Status.DISCONNECTED
                ? "Reintentar"
                : "Comprobar");
    }

    private void showConnectionIdle() {
        applyConnectionState("db-status-unknown", "Sin verificar");
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText("Comprobar");
        appContext.saveConnectionSnapshot(DbConnectionSnapshot.unknown());
    }

    private void showConnectionPending() {
        applyConnectionState("db-status-pending", "Comprobando…");
        connectionSpinner.setManaged(true);
        connectionSpinner.setVisible(true);
        verifyConnectionButton.setText("Comprobar");
    }

    private void showConnectionOk(DatabaseSettings settings) {
        applyConnectionState("db-status-ok", "Conectada");
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText("Comprobar");
        appContext.saveConnectionSnapshot(DbConnectionSnapshot.connected(
                settings.host() + ":" + settings.port() + " · " + settings.database()));
    }

    private void showConnectionError() {
        applyConnectionState("db-status-error", "Sin conexión");
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText("Reintentar");
        appContext.saveConnectionSnapshot(DbConnectionSnapshot.disconnected("Sin conexión"));
    }

    private void applyConnectionState(String styleClass, String statusText) {
        connectionBar.getStyleClass().removeAll(
                "db-status-unknown", "db-status-ok", "db-status-error", "db-status-pending");
        connectionBar.getStyleClass().add(styleClass);
        connectionStatusLabel.setText(statusText);
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
    }
}
