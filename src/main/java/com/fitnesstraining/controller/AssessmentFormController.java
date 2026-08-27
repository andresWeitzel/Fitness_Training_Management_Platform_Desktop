package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.assessments.dto.AssessmentClientOption;
import com.fitnesstraining.assessments.dto.AssessmentRequest;
import com.fitnesstraining.assessments.dto.AssessmentView;
import com.fitnesstraining.assessments.service.AssessmentService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AssessmentFormController {

    private static final NumberFormat ONE_DECIMAL = NumberFormat.getNumberInstance(new Locale("es", "AR"));

    static {
        ONE_DECIMAL.setMinimumFractionDigits(0);
        ONE_DECIMAL.setMaximumFractionDigits(1);
    }

    @FXML private Label subtitleLabel;
    @FXML private ComboBox<AssessmentClientOption> clientCombo;
    @FXML private DatePicker assessedDatePicker;
    @FXML private TextField weightField;
    @FXML private TextField heightField;
    @FXML private TextField bodyFatField;
    @FXML private TextField waistField;
    @FXML private TextField hipField;
    @FXML private TextField chestField;
    @FXML private Label bmiPreviewLabel;
    @FXML private TextArea notesField;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private Stage stage;
    private AssessmentService assessmentService;
    private Long assessorUserId;
    private Consumer<AssessmentView> onSaved;

    public static void open(
            Window owner,
            AssessmentService assessmentService,
            Long assessorUserId,
            List<AssessmentClientOption> clients,
            Long preselectedClientId,
            Consumer<AssessmentView> onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AssessmentFormController.class.getResource("/views/assessment-form.fxml"));
            Parent root = loader.load();
            AssessmentFormController controller = loader.getController();
            controller.setup(assessmentService, assessorUserId, clients, preselectedClientId, onSaved);
            controller.stage = DetailWindows.open(owner, root, "Nueva evaluación", 520);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el formulario de evaluación.", ex);
        }
    }

    private void setup(
            AssessmentService assessmentService,
            Long assessorUserId,
            List<AssessmentClientOption> clients,
            Long preselectedClientId,
            Consumer<AssessmentView> onSaved) {
        this.assessmentService = assessmentService;
        this.assessorUserId = assessorUserId;
        this.onSaved = onSaved;

        clientCombo.setItems(FXCollections.observableArrayList(clients));
        clientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AssessmentClientOption option) {
                return option == null ? "" : formatClient(option);
            }

            @Override
            public AssessmentClientOption fromString(String string) {
                return null;
            }
        });
        clientCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AssessmentClientOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatClient(item));
            }
        });

        if (preselectedClientId != null) {
            clients.stream()
                    .filter(c -> c.id().equals(preselectedClientId))
                    .findFirst()
                    .ifPresent(clientCombo::setValue);
        }

        assessedDatePicker.setValue(LocalDate.now());
        weightField.textProperty().addListener((obs, old, value) -> updateBmiPreview());
        heightField.textProperty().addListener((obs, old, value) -> updateBmiPreview());
        updateBmiPreview();
    }

    @FXML
    public void onSave() {
        try {
            AssessmentClientOption client = clientCombo.getValue();
            if (client == null) {
                statusError("Seleccione un cliente.");
                return;
            }
            AssessmentView view = assessmentService.register(
                    new AssessmentRequest(
                            client.id(),
                            assessedDatePicker.getValue(),
                            parseOptionalDecimal(weightField.getText(), "peso"),
                            parseOptionalDecimal(heightField.getText(), "altura"),
                            parseOptionalDecimal(bodyFatField.getText(), "grasa"),
                            parseOptionalDecimal(waistField.getText(), "cintura"),
                            parseOptionalDecimal(hipField.getText(), "cadera"),
                            parseOptionalDecimal(chestField.getText(), "tórax"),
                            notesField.getText()),
                    assessorUserId);
            if (onSaved != null) {
                onSaved.accept(view);
            }
            if (stage != null) {
                stage.close();
            }
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
        } catch (RuntimeException ex) {
            statusError("No se pudo registrar: " + ex.getMessage());
        }
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void updateBmiPreview() {
        BigDecimal weight = parseOptionalDecimalSilently(weightField.getText());
        BigDecimal height = parseOptionalDecimalSilently(heightField.getText());
        BigDecimal bmi = AssessmentService.computeBmi(weight, height);
        if (bmi == null) {
            bmiPreviewLabel.setText("Complete peso y altura para calcular el IMC.");
            return;
        }
        bmiPreviewLabel.setText("IMC estimado: "
                + ONE_DECIMAL.format(bmi)
                + " · "
                + AssessmentService.labelForBmi(bmi));
    }

    private void statusError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("status-info");
        if (!statusLabel.getStyleClass().contains("status-error")) {
            statusLabel.getStyleClass().add("status-error");
        }
    }

    private static String formatClient(AssessmentClientOption option) {
        String number = option.clientNumber() == null ? "" : " · N° " + option.clientNumber();
        return option.fullName() + " (" + option.documentNumber() + ")" + number;
    }

    private static BigDecimal parseOptionalDecimal(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new ValidationException("El valor de " + label + " no es válido.");
        }
    }

    private static BigDecimal parseOptionalDecimalSilently(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
