package com.fitnesstraining.controller;

import com.fitnesstraining.app.WindowChrome;
import com.fitnesstraining.controller.TrainingController.RoutineItemRow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;

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

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            stage.setTitle("Detalle del ejercicio");

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add(Objects.requireNonNull(
                    RoutineItemDetailController.class.getResource("/css/app.css")).toExternalForm());
            stage.setScene(scene);
            WindowChrome.makeDraggable(stage, root.lookup(".window-drag"));

            controller.stage = stage;
            stage.show();

            // Layout estable: medir contenido opaco y fijar tamaño (sin packStage genérico).
            Platform.runLater(() -> {
                root.applyCss();
                root.layout();
                double width = Math.ceil(root.prefWidth(-1));
                double height = Math.ceil(root.prefHeight(CARD_WIDTH));
                stage.setWidth(width);
                stage.setHeight(height);
                if (owner != null) {
                    placeOverOwner(stage, owner);
                } else {
                    stage.centerOnScreen();
                }
                stage.requestFocus();
            });
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle del ejercicio.", ex);
        }
    }

    private static void placeOverOwner(Stage stage, Window owner) {
        double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
        double y = owner.getY() + Math.max(56, (owner.getHeight() - stage.getHeight()) / 3);
        stage.setX(Math.max(owner.getX() + 12, x));
        stage.setY(Math.max(owner.getY() + 12, y));
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
