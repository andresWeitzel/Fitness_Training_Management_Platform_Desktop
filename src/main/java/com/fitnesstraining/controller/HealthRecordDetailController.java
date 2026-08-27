package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.nutrition.dto.HealthRecordView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class HealthRecordDetailController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label allergiesLabel;
    @FXML private Label restrictionsLabel;
    @FXML private Label conditionsLabel;
    @FXML private Label medicationsLabel;
    @FXML private Label notesLabel;

    private Stage stage;

    public static void open(Window owner, HealthRecordView view) {
        if (view == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    HealthRecordDetailController.class.getResource("/views/health-record-detail.fxml"));
            Parent root = loader.load();
            HealthRecordDetailController controller = loader.getController();
            controller.bind(view);
            controller.stage = DetailWindows.open(owner, root, "Ficha de salud", 480);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle de ficha de salud.", ex);
        }
    }

    private void bind(HealthRecordView view) {
        titleLabel.setText(view.clientName());
        summaryLabel.setText("Registro del "
                + DATE_FORMAT.format(view.recordedAt().toLocalDate())
                + " · "
                + view.recordedByName());
        allergiesLabel.setText(orEmpty(view.allergies()));
        restrictionsLabel.setText(orEmpty(view.restrictions()));
        conditionsLabel.setText(orEmpty(view.conditions()));
        medicationsLabel.setText(orEmpty(view.medications()));
        notesLabel.setText(orEmpty(view.notes()));
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private static String orEmpty(String value) {
        return value == null || value.isBlank() ? "Sin información." : value;
    }
}
