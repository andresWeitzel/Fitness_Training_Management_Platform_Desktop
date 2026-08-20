package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.shared.config.AppProperties;
import com.fitnesstraining.shared.config.DatabaseBootstrap;
import com.fitnesstraining.shared.config.DatabaseConfigStore;
import com.fitnesstraining.shared.config.DatabaseSettings;
import com.fitnesstraining.shared.exception.AppException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class DbSetupController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField databaseField;
    @FXML private TextField userField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button continueButton;

    private final AppContext appContext;
    private final DatabaseConfigStore configStore;
    private final DatabaseBootstrap bootstrap;
    private final AppProperties properties;

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
        statusLabel.setText("La configuración se guarda en " + configStore.location());
    }

    public void prepare(String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            statusLabel.getStyleClass().setAll("status-error");
            statusLabel.setText(errorMessage);
        }
    }

    @FXML
    public void onTestConnection() {
        try {
            bootstrap.testConnection(readSettings());
            statusLabel.getStyleClass().setAll("status-ok");
            statusLabel.setText("Conexión a PostgreSQL correcta.");
        } catch (Exception ex) {
            statusLabel.getStyleClass().setAll("status-error");
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void onContinue() {
        continueButton.setDisable(true);
        try {
            DatabaseSettings settings = readSettings();
            bootstrap.testConnection(settings);
            configStore.save(settings);
            appContext.connectAndShowLogin(settings);
        } catch (Exception ex) {
            statusLabel.getStyleClass().setAll("status-error");
            statusLabel.setText(ex.getMessage());
            continueButton.setDisable(false);
        }
    }

    @FXML
    public void onClose() {
        appContext.closeStage();
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
}
