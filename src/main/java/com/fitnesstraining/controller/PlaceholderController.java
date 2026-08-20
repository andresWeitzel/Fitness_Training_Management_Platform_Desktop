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
}
