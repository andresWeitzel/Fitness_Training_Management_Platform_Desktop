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
import com.fitnesstraining.shared.exception.AppException;
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
import javafx.scene.control.Tooltip;
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

    private Tooltip connectionTooltip;

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
        var startupError = appContext.consumePendingConnectionError();
        if (startupError.isPresent()) {
            showConnectionError(startupError.get());
        } else if (appContext.connectionSnapshot().status() == DbConnectionSnapshot.Status.UNKNOWN
                && !appContext.isDatabaseReady()) {
            showConnectionError("PostgreSQL no está disponible. Usá «Comprobar» para reintentar.");
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
        errorLabel.setText("");
        AuthService authService = appContext.authService().orElse(null);
        if (authService == null) {
            errorLabel.setText("La base de datos no está disponible. Comprobá la conexión.");
            showConnectionError("PostgreSQL no está disponible. Usá «Comprobar» para reintentar.");
            return;
        }
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

    @FXML
    public void onVerifyConnection() {
        configStore.load().ifPresentOrElse(this::runConnectionCheck, () -> showConnectionError(
                "No hay configuración de PostgreSQL. Completá la instalación inicial."));
    }

    private void runConnectionCheck(DatabaseSettings settings) {
        verifyConnectionButton.setDisable(true);
        loginButton.setDisable(true);
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
            } catch (Exception ex) {
                String message = ex instanceof AppException appEx
                        ? appEx.getMessage()
                        : "Conexión correcta, pero no se pudo preparar la aplicación: " + ex.getMessage();
                showConnectionError(message);
            } finally {
                verifyConnectionButton.setDisable(false);
                loginButton.setDisable(false);
                fitWindow();
            }
        }));
        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            String message = ex instanceof AppException appEx
                    ? appEx.getMessage()
                    : "No se pudo conectar: " + ex.getMessage();
            showConnectionError(message);
            verifyConnectionButton.setDisable(false);
            loginButton.setDisable(false);
            fitWindow();
        }));
        new Thread(task, "login-connection-check").start();
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
            case CONNECTED -> applyConnectionState("db-status-ok", "Conectada", snapshot.tooltip());
            case DISCONNECTED -> applyConnectionState("db-status-error", "Sin conexión", snapshot.tooltip());
            default -> showConnectionIdle();
        }
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText(snapshot.status() == DbConnectionSnapshot.Status.DISCONNECTED
                ? "Reintentar"
                : "Comprobar");
    }

    private void showConnectionIdle() {
        applyConnectionState("db-status-unknown", "Sin verificar", null);
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText("Comprobar");
        appContext.saveConnectionSnapshot(DbConnectionSnapshot.unknown());
    }

    private void showConnectionPending() {
        applyConnectionState("db-status-pending", "Comprobando…", null);
        connectionSpinner.setManaged(true);
        connectionSpinner.setVisible(true);
        verifyConnectionButton.setText("Comprobando…");
    }

    private void showConnectionOk(DatabaseSettings settings) {
        String tooltip = tooltipFor(settings);
        applyConnectionState("db-status-ok", "Conectada", tooltip);
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText("Comprobar");
        appContext.saveConnectionSnapshot(DbConnectionSnapshot.connected(tooltip));
    }

    private void showConnectionError(String details) {
        applyConnectionState("db-status-error", "Sin conexión", details);
        connectionSpinner.setManaged(false);
        connectionSpinner.setVisible(false);
        verifyConnectionButton.setText("Reintentar");
        appContext.saveConnectionSnapshot(DbConnectionSnapshot.disconnected(details));
    }

    private void applyConnectionState(String styleClass, String statusText, String tooltipText) {
        connectionBar.getStyleClass().removeAll(
                "db-status-unknown", "db-status-ok", "db-status-error", "db-status-pending");
        connectionBar.getStyleClass().add(styleClass);
        connectionStatusLabel.setText(statusText);
        if (connectionTooltip != null) {
            Tooltip.uninstall(connectionBar, connectionTooltip);
            connectionTooltip = null;
        }
        if (tooltipText != null && !tooltipText.isBlank()) {
            connectionTooltip = new Tooltip(tooltipText);
            connectionTooltip.setWrapText(true);
            connectionTooltip.setMaxWidth(360);
            Tooltip.install(connectionBar, connectionTooltip);
        }
    }

    private static String tooltipFor(DatabaseSettings settings) {
        return settings.host() + ":" + settings.port() + " · " + settings.database();
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
    }
}
