package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.nutrition.dto.NutritionPlanView;
import com.fitnesstraining.nutrition.service.NutritionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class NutritionPlanDetailController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label validityLabel;
    @FXML private Label objectivesLabel;
    @FXML private Label mealGuidanceLabel;
    @FXML private Label notesLabel;

    private Stage stage;

    public static void open(Window owner, NutritionPlanView view) {
        if (view == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    NutritionPlanDetailController.class.getResource("/views/nutrition-plan-detail.fxml"));
            Parent root = loader.load();
            NutritionPlanDetailController controller = loader.getController();
            controller.bind(view);
            controller.stage = DetailWindows.open(owner, root, "Detalle del plan", 500);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle del plan.", ex);
        }
    }

    private void bind(NutritionPlanView view) {
        titleLabel.setText(view.title());
        summaryLabel.setText(view.clientName()
                + " · "
                + view.createdByName()
                + (view.clientNumber() == null || view.clientNumber().isBlank()
                ? ""
                : " · N° " + view.clientNumber()));
        statusLabel.setText(NutritionService.labelForPlanStatus(view.status()));
        statusLabel.getStyleClass().setAll(NutritionService.badgeClassForPlanStatus(view.status()));
        String from = view.validFrom() == null ? "—" : DATE_FORMAT.format(view.validFrom());
        String until = view.validUntil() == null ? "—" : DATE_FORMAT.format(view.validUntil());
        validityLabel.setText(from + " → " + until);
        objectivesLabel.setText(orEmpty(view.objectives()));
        mealGuidanceLabel.setText(orEmpty(view.mealGuidance()));
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
