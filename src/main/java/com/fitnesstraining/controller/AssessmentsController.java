package com.fitnesstraining.controller;

import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.assessments.dto.AssessmentClientOption;
import com.fitnesstraining.assessments.dto.AssessmentSummary;
import com.fitnesstraining.assessments.model.AssessmentListScope;
import com.fitnesstraining.assessments.service.AssessmentService;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AssessmentsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat ONE_DECIMAL = NumberFormat.getNumberInstance(new Locale("es", "AR"));

    static {
        ONE_DECIMAL.setMinimumFractionDigits(0);
        ONE_DECIMAL.setMaximumFractionDigits(1);
    }

    @FXML private Label countLabel;
    @FXML private Label historyHintLabel;
    @FXML private Button filterAllButton;
    @FXML private Button filter30Button;
    @FXML private Button filter90Button;
    @FXML private ComboBox<AssessmentClientOption> historyClientCombo;
    @FXML private TextField searchField;
    @FXML private Button registerForClientButton;
    @FXML private TableView<AssessmentSummary> assessmentsTable;
    @FXML private TableColumn<AssessmentSummary, String> dateColumn;
    @FXML private TableColumn<AssessmentSummary, String> clientColumn;
    @FXML private TableColumn<AssessmentSummary, String> weightColumn;
    @FXML private TableColumn<AssessmentSummary, String> bmiColumn;
    @FXML private TableColumn<AssessmentSummary, String> fatColumn;
    @FXML private TableColumn<AssessmentSummary, String> assessorColumn;
    @FXML private TableColumn<AssessmentSummary, String> detailColumn;

    private final AssessmentService assessmentService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    private boolean canManage;
    private AssessmentListScope scope = AssessmentListScope.ALL;
    private List<AssessmentClientOption> activeClients = List.of();
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(180));

    public AssessmentsController(
            AssessmentService assessmentService,
            SessionContext sessionContext,
            AuthorizationService authorizationService) {
        this.assessmentService = assessmentService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.ASSESSMENTS_MANAGE);
        applyPermissions();
        setupTable();
        setupClientCombo();

        searchDelay.setOnFinished(e -> refreshAssessments());
        searchField.textProperty().addListener((obs, old, value) -> {
            searchDelay.stop();
            searchDelay.playFromStart();
        });
        historyClientCombo.valueProperty().addListener((obs, old, client) -> refreshAssessments());

        assessmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                historyHintLabel.setText("Historial de " + selected.clientName()
                        + ". Use ⓘ para ver el detalle completo.");
            }
        });

        Platform.runLater(this::loadInitialData);
    }

    @FXML
    public void onFilterAll() {
        setScope(AssessmentListScope.ALL);
    }

    @FXML
    public void onFilter30Days() {
        setScope(AssessmentListScope.LAST_30_DAYS);
    }

    @FXML
    public void onFilter90Days() {
        setScope(AssessmentListScope.LAST_90_DAYS);
    }

    @FXML
    public void onNewAssessment() {
        if (!canManage) {
            return;
        }
        Long preselected = null;
        AssessmentSummary selected = assessmentsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            preselected = selected.clientId();
        } else if (historyClientCombo.getValue() != null) {
            preselected = historyClientCombo.getValue().id();
        }
        Window owner = assessmentsTable.getScene() == null ? null : assessmentsTable.getScene().getWindow();
        AssessmentFormController.open(
                owner,
                assessmentService,
                sessionContext.requireUser().id(),
                activeClients,
                preselected,
                view -> {
                    refreshAssessments();
                    selectAssessmentInTable(view.id());
                    historyHintLabel.setText("Evaluación registrada (#" + view.id() + ").");
                });
    }

    private void loadInitialData() {
        try {
            activeClients = assessmentService.listActiveClients();
            historyClientCombo.setItems(FXCollections.observableArrayList(activeClients));
            refreshAssessments();
        } catch (RuntimeException ex) {
            historyHintLabel.setText("Error al cargar evaluaciones: " + ex.getMessage());
        }
    }

    private void setupTable() {
        assessmentsTable.setPlaceholder(new Label("No hay evaluaciones para este filtro."));
        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(DATE_FORMAT.format(data.getValue().assessedAt().toLocalDate())));
        clientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        weightColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDecimal(data.getValue().weightKg(), "kg")));
        bmiColumn.setCellValueFactory(data -> new SimpleStringProperty(formatBmi(data.getValue().bmi())));
        fatColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDecimal(data.getValue().bodyFatPct(), "%")));
        assessorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().assessorName()));
        detailColumn.setCellFactory(col -> detailActionCell());
        detailColumn.setSortable(false);
    }

    private TableCell<AssessmentSummary, String> detailActionCell() {
        return new TableCell<>() {
            private final Button detail = new Button("ⓘ");

            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver detalle de la evaluación"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < assessmentsTable.getItems().size()) {
                        openDetail(assessmentsTable.getItems().get(getIndex()).id());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= assessmentsTable.getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private void openDetail(Long assessmentId) {
        try {
            var view = assessmentService.get(assessmentId);
            Window owner = assessmentsTable.getScene() == null ? null : assessmentsTable.getScene().getWindow();
            AssessmentDetailController.open(owner, view);
        } catch (RuntimeException ex) {
            historyHintLabel.setText(ex.getMessage());
        }
    }

    private void setupClientCombo() {
        historyClientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AssessmentClientOption option) {
                return option == null ? "" : formatClient(option);
            }

            @Override
            public AssessmentClientOption fromString(String string) {
                return null;
            }
        });
        historyClientCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AssessmentClientOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatClient(item));
            }
        });
    }

    private void refreshAssessments() {
        Long clientId = historyClientCombo.getValue() == null ? null : historyClientCombo.getValue().id();
        List<AssessmentSummary> rows = assessmentService.list(searchField.getText(), scope, clientId);
        assessmentsTable.setItems(FXCollections.observableArrayList(rows));
        String clientSuffix = historyClientCombo.getValue() == null
                ? ""
                : " · " + historyClientCombo.getValue().fullName();
        countLabel.setText(rows.size() + (rows.size() == 1 ? " evaluación" : " evaluaciones")
                + scopeLabel(scope)
                + clientSuffix);
        if (historyClientCombo.getValue() != null) {
            historyHintLabel.setText("Historial de " + historyClientCombo.getValue().fullName()
                    + ". Use ⓘ para ver el detalle completo.");
        } else if (assessmentsTable.getSelectionModel().getSelectedItem() == null) {
            historyHintLabel.setText("Seguimiento de peso, medidas e IMC. Use ⓘ para ver el detalle completo.");
        }
    }

    private void setScope(AssessmentListScope next) {
        scope = next;
        applyChipState(filterAllButton, next == AssessmentListScope.ALL);
        applyChipState(filter30Button, next == AssessmentListScope.LAST_30_DAYS);
        applyChipState(filter90Button, next == AssessmentListScope.LAST_90_DAYS);
        refreshAssessments();
    }

    private void selectAssessmentInTable(Long id) {
        assessmentsTable.getItems().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .ifPresent(row -> assessmentsTable.getSelectionModel().select(row));
    }

    private void applyPermissions() {
        registerForClientButton.setDisable(!canManage);
        if (!canManage) {
            historyHintLabel.setText("Solo lectura: no tiene permiso para registrar evaluaciones.");
        }
    }

    private static void applyChipState(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private static String scopeLabel(AssessmentListScope scope) {
        return switch (scope) {
            case ALL -> "";
            case LAST_30_DAYS -> " · últimos 30 días";
            case LAST_90_DAYS -> " · últimos 90 días";
        };
    }

    private static String formatClient(AssessmentClientOption option) {
        String number = option.clientNumber() == null ? "" : " · N° " + option.clientNumber();
        return option.fullName() + " (" + option.documentNumber() + ")" + number;
    }

    private static String formatDecimal(BigDecimal value, String unit) {
        if (value == null) {
            return "—";
        }
        return ONE_DECIMAL.format(value) + " " + unit;
    }

    private static String formatBmi(BigDecimal bmi) {
        if (bmi == null) {
            return "—";
        }
        return ONE_DECIMAL.format(bmi);
    }
}
