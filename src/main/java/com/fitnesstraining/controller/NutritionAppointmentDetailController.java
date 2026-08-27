package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentView;
import com.fitnesstraining.nutrition.service.NutritionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class NutritionAppointmentDetailController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label notesLabel;

    private Stage stage;

    public static void open(Window owner, NutritionAppointmentView view) {
        if (view == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    NutritionAppointmentDetailController.class.getResource("/views/nutrition-appointment-detail.fxml"));
            Parent root = loader.load();
            NutritionAppointmentDetailController controller = loader.getController();
            controller.bind(view);
            controller.stage = DetailWindows.open(owner, root, "Detalle de turno", 440);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle del turno.", ex);
        }
    }

    private void bind(NutritionAppointmentView view) {
        titleLabel.setText(view.clientName());
        summaryLabel.setText(DATE_TIME_FORMAT.format(view.scheduledAt())
                + " · "
                + view.nutritionistName()
                + (view.clientNumber() == null || view.clientNumber().isBlank()
                ? ""
                : " · N° " + view.clientNumber()));
        statusLabel.setText(NutritionService.labelForAppointmentStatus(view.status()));
        statusLabel.getStyleClass().setAll(NutritionService.badgeClassForAppointmentStatus(view.status()));
        notesLabel.setText(view.notes() == null || view.notes().isBlank() ? "Sin notas." : view.notes());
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
