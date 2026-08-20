package com.fitnesstraining.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PlaceholderController {

    @FXML private Label eyebrowLabel;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;

    public void show(String title, String description) {
        eyebrowLabel.setText("PRÓXIMO MÓDULO");
        titleLabel.setText(title);
        descriptionLabel.setText(description);
    }

    public void showError(String title, String message) {
        eyebrowLabel.setText("ERROR AL CARGAR");
        titleLabel.setText(title);
        descriptionLabel.setText(message == null || message.isBlank()
                ? "No se pudo abrir este módulo. Cierre sesión, vuelva a ejecutar run.bat y revise la consola."
                : message);
    }
}
