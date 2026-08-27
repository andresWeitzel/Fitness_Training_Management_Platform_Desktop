package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.assessments.dto.AssessmentView;
import com.fitnesstraining.assessments.service.AssessmentService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AssessmentDetailController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat ONE_DECIMAL = NumberFormat.getNumberInstance(new Locale("es", "AR"));

    static {
        ONE_DECIMAL.setMinimumFractionDigits(0);
        ONE_DECIMAL.setMaximumFractionDigits(1);
    }

    private static final double CARD_WIDTH = 460;

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label weightLabel;
    @FXML private Label heightLabel;
    @FXML private Label bmiLabel;
    @FXML private Label fatLabel;
    @FXML private Label waistLabel;
    @FXML private Label hipLabel;
    @FXML private Label chestLabel;
    @FXML private Label notesLabel;

    private Stage stage;

    public static void open(Window owner, AssessmentView view) {
        if (view == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    AssessmentDetailController.class.getResource("/views/assessment-detail.fxml"));
            Parent root = loader.load();
            AssessmentDetailController controller = loader.getController();
            controller.bind(view);
            controller.stage = DetailWindows.open(owner, root, "Detalle de evaluación", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle de evaluación.", ex);
        }
    }

    private void bind(AssessmentView view) {
        titleLabel.setText(view.clientName());
        summaryLabel.setText("Evaluación del "
                + DATE_FORMAT.format(view.assessedAt().toLocalDate())
                + " · Evaluó: "
                + view.assessorName());
        weightLabel.setText(formatDecimal(view.weightKg(), "kg"));
        heightLabel.setText(formatDecimal(view.heightCm(), "cm"));
        if (view.bmi() == null) {
            bmiLabel.setText("—");
        } else {
            bmiLabel.setText(ONE_DECIMAL.format(view.bmi()) + " · " + AssessmentService.labelForBmi(view.bmi()));
        }
        fatLabel.setText(formatDecimal(view.bodyFatPct(), "%"));
        waistLabel.setText(formatDecimal(view.waistCm(), "cm"));
        hipLabel.setText(formatDecimal(view.hipCm(), "cm"));
        chestLabel.setText(formatDecimal(view.chestCm(), "cm"));
        notesLabel.setText(view.notes() == null || view.notes().isBlank() ? "Sin notas." : view.notes());
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private static String formatDecimal(BigDecimal value, String unit) {
        if (value == null) {
            return "—";
        }
        return ONE_DECIMAL.format(value) + " " + unit;
    }
}
