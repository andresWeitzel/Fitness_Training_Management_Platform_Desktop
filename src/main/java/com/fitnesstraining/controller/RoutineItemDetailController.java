package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.controller.TrainingController.RoutineItemRow;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class RoutineItemDetailController {

    private static final double CARD_WIDTH = 400;

    @FXML private StackPane rootPane;
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label prescriptionLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label techniqueLabel;

    private Stage stage;

    public static void open(Window owner, RoutineItemRow row) {
        if (row == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    RoutineItemDetailController.class.getResource("/views/routine-item-detail.fxml"));
            Parent root = loader.load();
            RoutineItemDetailController controller = loader.getController();
            controller.bind(row);
            controller.stage = DetailWindows.open(owner, root, "Detalle del ejercicio", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle del ejercicio.", ex);
        }
    }

    private void bind(RoutineItemRow row) {
        titleLabel.setText(row.exerciseName());
        metaLabel.setText(buildMeta(row));
        prescriptionLabel.setText(buildPrescription(row));
        descriptionLabel.setText(blankOr(
                row.description(),
                "Sin descripción en el catálogo."));
        techniqueLabel.setText(blankOr(
                row.techniqueNotes(),
                "Sin notas técnicas en el catálogo."));
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private static String buildMeta(RoutineItemRow row) {
        StringBuilder meta = new StringBuilder();
        append(meta, row.muscleGroupLabel());
        append(meta, row.equipmentLabel());
        append(meta, row.difficultyLabel());
        if (row.secondaryMuscles() != null && !row.secondaryMuscles().isBlank()) {
            append(meta, "Secundarios: " + row.secondaryMuscles());
        }
        return meta.isEmpty() ? "—" : meta.toString();
    }

    private static String buildPrescription(RoutineItemRow row) {
        StringBuilder line = new StringBuilder();
        if (row.sets() != null) {
            line.append(row.sets()).append(" series");
        }
        if (row.reps() != null && !row.reps().isBlank()) {
            if (!line.isEmpty()) {
                line.append(" · ");
            }
            line.append(row.reps()).append(" reps");
        }
        if (row.restSeconds() != null) {
            if (!line.isEmpty()) {
                line.append(" · ");
            }
            line.append(row.restSeconds()).append("s descanso");
        }
        if (row.loadNote() != null && !row.loadNote().isBlank()) {
            if (!line.isEmpty()) {
                line.append(" · ");
            }
            line.append(row.loadNote());
        }
        return line.isEmpty() ? "Sin prescripción cargada." : line.toString();
    }

    private static void append(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" · ");
        }
        builder.append(value);
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
