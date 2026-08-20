package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.DbSetupMode;
import com.fitnesstraining.app.WindowChrome;
import com.fitnesstraining.shared.config.AppProperties;
import com.fitnesstraining.shared.config.DatabaseBootstrap;
import com.fitnesstraining.shared.config.DatabaseConfigStore;
import com.fitnesstraining.shared.config.DatabaseSettings;
import com.fitnesstraining.shared.exception.AppException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DbSetupController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField databaseField;
    @FXML private TextField userField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Label configPathLabel;
    @FXML private Button testButton;
    @FXML private Button continueButton;
    @FXML private Button backButton;
    @FXML private HBox statusBox;
    @FXML private ProgressIndicator statusSpinner;
    @FXML private StackPane rootPane;

    private final AppContext appContext;
    private final DatabaseConfigStore configStore;
    private final DatabaseBootstrap bootstrap;
    private final AppProperties properties;

    private DbSetupMode mode = DbSetupMode.INSTALL;
    private boolean lastTestSucceeded;

    public DbSetupController(
            AppContext appContext,
            DatabaseConfigStore configStore,
            DatabaseBootstrap bootstrap,
            AppProperties properties) {
        this.appContext = appContext;
        this.configStore = configStore;
        this.bootstrap = bootstrap;
        this.properties = properties;
    }

    @FXML
    public void initialize() {
        configStore.load().ifPresentOrElse(this::fill, () -> fill(properties.toDatabaseSettings()));
        if (configPathLabel != null) {
            configPathLabel.setText("Archivo: " + configStore.location());
        }
        continueButton.setDisable(true);
        fitWindow();
    }

    public void prepare(DbSetupMode mode, String errorMessage) {
        this.mode = mode;
        applyMode();
        if (errorMessage != null && !errorMessage.isBlank()) {
            showStatus("error", errorMessage);
        } else if (mode == DbSetupMode.INSTALL) {
            showStatus("info", "Completá los datos y probá la conexión antes de continuar.");
        }
        fitWindow();
    }

    @FXML
    public void onTestConnection() {
        runConnectionTest(readSettings(), false);
    }

    @FXML
    public void onContinue() {
        DatabaseSettings settings = readSettings();
        continueButton.setDisable(true);
        testButton.setDisable(true);
        showStatus("pending", mode == DbSetupMode.INSTALL
                ? "Guardando configuración y preparando la base de datos…"
                : "Guardando y reconectando…");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                bootstrap.testConnection(settings);
                return null;
            }
        };
        task.setOnSucceeded(event -> Platform.runLater(() -> {
            try {
                if (mode == DbSetupMode.INSTALL) {
                    appContext.connectAndShowLogin(settings);
                } else {
                    appContext.reconnectFromAdmin(settings);
                    showStatus("ok", "Conexión actualizada correctamente.");
                }
            } catch (Exception ex) {
                String message = ex instanceof AppException appEx
                        ? appEx.getMessage()
                        : "No se pudo aplicar la configuración: " + ex.getMessage();
                showStatus("error", message);
                continueButton.setDisable(!lastTestSucceeded);
            } finally {
                testButton.setDisable(false);
                if (mode == DbSetupMode.INSTALL) {
                    continueButton.setDisable(false);
                } else if (lastTestSucceeded) {
                    continueButton.setDisable(true);
                }
                fitWindow();
            }
        }));
        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            String message = ex instanceof AppException appEx
                    ? appEx.getMessage()
                    : "No se pudo aplicar la configuración: " + ex.getMessage();
            showStatus("error", message);
            continueButton.setDisable(!lastTestSucceeded);
            testButton.setDisable(false);
            fitWindow();
        }));
        new Thread(task, "db-setup-continue").start();
    }

    @FXML
    public void onBackToLogin() {
        appContext.returnToLogin();
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
    }

    private void applyMode() {
        if (mode == DbSetupMode.INSTALL) {
            continueButton.setText("Continuar al login");
            if (backButton != null) {
                backButton.setVisible(appContext.canReturnToLogin());
                backButton.setManaged(appContext.canReturnToLogin());
            }
        } else {
            continueButton.setText("Guardar y reconectar");
            if (backButton != null) {
                backButton.setVisible(false);
                backButton.setManaged(false);
            }
        }
    }

    private void runConnectionTest(DatabaseSettings settings, boolean silent) {
        testButton.setDisable(true);
        continueButton.setDisable(true);
        if (!silent) {
            showStatus("pending", "Comprobando conexión con PostgreSQL…");
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                bootstrap.testConnection(settings);
                return null;
            }
        };
        task.setOnSucceeded(event -> Platform.runLater(() -> {
            lastTestSucceeded = true;
            showStatus("ok", "Conexión a PostgreSQL correcta.");
            continueButton.setDisable(false);
            testButton.setDisable(false);
            fitWindow();
        }));
        task.setOnFailed(event -> Platform.runLater(() -> {
            lastTestSucceeded = false;
            Throwable ex = task.getException();
            String message = ex instanceof AppException appEx
                    ? appEx.getMessage()
                    : "No se pudo conectar: " + ex.getMessage();
            showStatus("error", message);
            continueButton.setDisable(true);
            testButton.setDisable(false);
            fitWindow();
        }));
        new Thread(task, "db-setup-test").start();
    }

    private void showStatus(String kind, String message) {
        if (statusBox == null || statusLabel == null) {
            return;
        }
        statusBox.setManaged(true);
        statusBox.setVisible(true);
        statusBox.getStyleClass().removeAll("connection-status-ok", "connection-status-error", "connection-status-pending", "connection-status-info");
        statusLabel.getStyleClass().removeAll("status-ok", "status-error", "status-pending");

        switch (kind) {
            case "ok" -> {
                statusBox.getStyleClass().add("connection-status-ok");
                statusLabel.getStyleClass().add("status-ok");
            }
            case "error" -> {
                statusBox.getStyleClass().add("connection-status-error");
                statusLabel.getStyleClass().add("status-error");
            }
            case "pending" -> {
                statusBox.getStyleClass().add("connection-status-pending");
                statusLabel.getStyleClass().add("status-pending");
            }
            default -> statusBox.getStyleClass().add("connection-status-info");
        }

        boolean pending = "pending".equals(kind);
        if (statusSpinner != null) {
            statusSpinner.setManaged(pending);
            statusSpinner.setVisible(pending);
        }
        statusLabel.setText(message);
    }

    private void fill(DatabaseSettings settings) {
        hostField.setText(settings.host());
        portField.setText(String.valueOf(settings.port()));
        databaseField.setText(settings.database());
        userField.setText(settings.username());
        passwordField.setText(settings.password());
    }

    private DatabaseSettings readSettings() {
        try {
            return new DatabaseSettings(
                    hostField.getText(),
                    Integer.parseInt(portField.getText().trim()),
                    databaseField.getText(),
                    userField.getText(),
                    passwordField.getText() == null ? "" : passwordField.getText()
            );
        } catch (NumberFormatException ex) {
            throw new AppException("El puerto debe ser un número.");
        }
    }

    private void fitWindow() {
        if (mode == DbSetupMode.INSTALL && rootPane instanceof StackPane stackPane) {
            WindowChrome.fitStage(stackPane);
        }
    }
}
