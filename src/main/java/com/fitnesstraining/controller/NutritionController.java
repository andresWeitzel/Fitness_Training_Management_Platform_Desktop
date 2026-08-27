package com.fitnesstraining.controller;

import com.fitnesstraining.app.ConfirmDialogs;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.app.TableStatusCells;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.nutrition.dto.HealthRecordRequest;
import com.fitnesstraining.nutrition.dto.HealthRecordSummary;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentRequest;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentSummary;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentView;
import com.fitnesstraining.nutrition.dto.NutritionClientOption;
import com.fitnesstraining.nutrition.dto.NutritionPlanRequest;
import com.fitnesstraining.nutrition.dto.NutritionPlanSummary;
import com.fitnesstraining.nutrition.dto.NutritionPlanView;
import com.fitnesstraining.nutrition.model.NutritionAppointmentListScope;
import com.fitnesstraining.nutrition.model.NutritionAppointmentStatus;
import com.fitnesstraining.nutrition.model.NutritionPlanListScope;
import com.fitnesstraining.nutrition.model.NutritionPlanStatus;
import com.fitnesstraining.nutrition.service.NutritionService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NutritionController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TabPane nutritionTabs;

    @FXML private Label appointmentsCountLabel;
    @FXML private Button appointmentAllButton;
    @FXML private Button appointmentUpcomingButton;
    @FXML private Button appointment30Button;
    @FXML private TextField appointmentSearchField;
    @FXML private Button newAppointmentButton;
    @FXML private TableView<NutritionAppointmentSummary> appointmentsTable;
    @FXML private TableColumn<NutritionAppointmentSummary, String> appointmentDateColumn;
    @FXML private TableColumn<NutritionAppointmentSummary, String> appointmentClientColumn;
    @FXML private TableColumn<NutritionAppointmentSummary, String> appointmentStatusColumn;
    @FXML private TableColumn<NutritionAppointmentSummary, String> appointmentNutritionistColumn;
    @FXML private TableColumn<NutritionAppointmentSummary, String> appointmentDetailColumn;
    @FXML private Label appointmentSubtitleLabel;
    @FXML private Label appointmentStatusBadge;
    @FXML private ComboBox<NutritionClientOption> appointmentClientCombo;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private TextField appointmentTimeField;
    @FXML private TextArea appointmentNotesField;
    @FXML private Label appointmentStatusLabel;
    @FXML private Button saveAppointmentButton;
    @FXML private Button viewAppointmentButton;
    @FXML private Button completeAppointmentButton;
    @FXML private Button cancelAppointmentButton;
    @FXML private Button noShowAppointmentButton;

    @FXML private Label plansCountLabel;
    @FXML private Button planActiveButton;
    @FXML private Button planDraftButton;
    @FXML private Button planArchivedButton;
    @FXML private Button planAllButton;
    @FXML private TextField planSearchField;
    @FXML private Button newPlanButton;
    @FXML private TableView<NutritionPlanSummary> plansTable;
    @FXML private TableColumn<NutritionPlanSummary, String> planClientColumn;
    @FXML private TableColumn<NutritionPlanSummary, String> planTitleColumn;
    @FXML private TableColumn<NutritionPlanSummary, String> planValidityColumn;
    @FXML private TableColumn<NutritionPlanSummary, String> planStatusColumn;
    @FXML private TableColumn<NutritionPlanSummary, String> planDetailColumn;
    @FXML private Label planSubtitleLabel;
    @FXML private Label planStatusBadge;
    @FXML private ComboBox<NutritionClientOption> planClientCombo;
    @FXML private TextField planTitleField;
    @FXML private TextArea planObjectivesField;
    @FXML private TextArea planMealGuidanceField;
    @FXML private ComboBox<NutritionPlanStatus> planStatusCombo;
    @FXML private DatePicker planValidFromPicker;
    @FXML private DatePicker planValidUntilPicker;
    @FXML private TextArea planNotesField;
    @FXML private Label planStatusLabel;
    @FXML private Button savePlanButton;
    @FXML private Button viewPlanButton;
    @FXML private Button archivePlanButton;

    @FXML private Label healthCountLabel;
    @FXML private Button healthByClientButton;
    @FXML private Button healthAllButton;
    @FXML private ComboBox<NutritionClientOption> healthClientCombo;
    @FXML private TextField healthSearchField;
    @FXML private TableView<HealthRecordSummary> healthTable;
    @FXML private TableColumn<HealthRecordSummary, String> healthDateColumn;
    @FXML private TableColumn<HealthRecordSummary, String> healthClientColumn;
    @FXML private TableColumn<HealthRecordSummary, String> healthAllergiesColumn;
    @FXML private TableColumn<HealthRecordSummary, String> healthRestrictionsColumn;
    @FXML private TableColumn<HealthRecordSummary, String> healthAuthorColumn;
    @FXML private TableColumn<HealthRecordSummary, String> healthDetailColumn;
    @FXML private Label healthSubtitleLabel;
    @FXML private ComboBox<NutritionClientOption> healthEntryClientCombo;
    @FXML private DatePicker healthDatePicker;
    @FXML private TextArea healthAllergiesField;
    @FXML private TextArea healthRestrictionsField;
    @FXML private TextArea healthConditionsField;
    @FXML private TextArea healthMedicationsField;
    @FXML private TextArea healthNotesField;
    @FXML private Label healthStatusLabel;
    @FXML private Button saveHealthButton;

    private final NutritionService nutritionService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    private boolean canManage;
    private List<NutritionClientOption> activeClients = List.of();
    private NutritionAppointmentListScope appointmentScope = NutritionAppointmentListScope.ALL;
    private NutritionPlanListScope planScope = NutritionPlanListScope.ACTIVE;
    private Long selectedAppointmentId;
    private Long selectedPlanId;
    private boolean healthShowAll;

    private final PauseTransition appointmentSearchDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition planSearchDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition healthSearchDelay = new PauseTransition(Duration.millis(180));

    public NutritionController(
            NutritionService nutritionService,
            SessionContext sessionContext,
            AuthorizationService authorizationService) {
        this.nutritionService = nutritionService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.NUTRITION_MANAGE);
        applyPermissions();
        setupTables();
        setupCombos();

        appointmentSearchDelay.setOnFinished(e -> refreshAppointments());
        appointmentSearchField.textProperty().addListener((obs, old, value) -> {
            appointmentSearchDelay.stop();
            appointmentSearchDelay.playFromStart();
        });
        planSearchDelay.setOnFinished(e -> refreshPlans());
        planSearchField.textProperty().addListener((obs, old, value) -> {
            planSearchDelay.stop();
            planSearchDelay.playFromStart();
        });
        healthSearchDelay.setOnFinished(e -> refreshHealthRecords());
        healthSearchField.textProperty().addListener((obs, old, value) -> {
            healthSearchDelay.stop();
            healthSearchDelay.playFromStart();
        });

        appointmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadAppointment(selected.id());
            }
        });
        plansTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadPlan(selected.id());
            }
        });
        healthClientCombo.valueProperty().addListener((obs, old, client) -> {
            if (!healthShowAll) {
                if (client != null) {
                    healthEntryClientCombo.setValue(client);
                }
                refreshHealthRecords();
            }
        });
        healthTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                selectClient(healthEntryClientCombo, selected.clientId());
            }
        });

        onNewAppointment();
        onNewPlan();
        clearHealthForm();
        Platform.runLater(this::loadInitialData);
    }

    @FXML public void onAppointmentFilterAll() { setAppointmentScope(NutritionAppointmentListScope.ALL); }
    @FXML public void onAppointmentFilterUpcoming() { setAppointmentScope(NutritionAppointmentListScope.UPCOMING); }
    @FXML public void onAppointmentFilter30Days() { setAppointmentScope(NutritionAppointmentListScope.LAST_30_DAYS); }
    @FXML public void onPlanFilterActive() { setPlanScope(NutritionPlanListScope.ACTIVE); }
    @FXML public void onPlanFilterDraft() { setPlanScope(NutritionPlanListScope.DRAFT); }
    @FXML public void onPlanFilterArchived() { setPlanScope(NutritionPlanListScope.ARCHIVED); }
    @FXML public void onPlanFilterAll() { setPlanScope(NutritionPlanListScope.ALL); }
    @FXML public void onHealthFilterByClient() { setHealthScope(false); }
    @FXML public void onHealthFilterAll() { setHealthScope(true); }

    @FXML
    public void onViewAppointmentDetail() {
        if (selectedAppointmentId == null) {
            showAppointmentError("Seleccione un turno del historial.");
            return;
        }
        openAppointmentDetail(selectedAppointmentId);
    }

    @FXML
    public void onViewPlanDetail() {
        if (selectedPlanId == null) {
            showPlanError("Seleccione un plan del historial.");
            return;
        }
        openPlanDetail(selectedPlanId);
    }

    @FXML
    public void onNewAppointment() {
        appointmentsTable.getSelectionModel().clearSelection();
        selectedAppointmentId = null;
        appointmentClientCombo.setValue(null);
        appointmentDatePicker.setValue(LocalDate.now().plusDays(1));
        appointmentTimeField.setText("10:00");
        appointmentNotesField.clear();
        appointmentSubtitleLabel.setText("Agende una consulta nutricional.");
        appointmentStatusBadge.setText("Nuevo");
        appointmentStatusBadge.getStyleClass().setAll("badge-ready");
        appointmentStatusLabel.setText("");
        updateAppointmentActions(null);
    }

    @FXML
    public void onSaveAppointment() {
        if (!canManage) {
            return;
        }
        try {
            NutritionClientOption client = appointmentClientCombo.getValue();
            if (client == null) {
                showAppointmentError("Seleccione un cliente.");
                return;
            }
            NutritionAppointmentRequest request = new NutritionAppointmentRequest(
                    client.id(),
                    appointmentDatePicker.getValue(),
                    parseTime(appointmentTimeField.getText()),
                    appointmentNotesField.getText());
            NutritionAppointmentView view;
            if (selectedAppointmentId == null) {
                view = nutritionService.scheduleAppointment(request, sessionContext.requireUser().id());
                showAppointmentInfo("Turno agendado (#" + view.id() + ").");
            } else {
                view = nutritionService.rescheduleAppointment(selectedAppointmentId, request);
                showAppointmentInfo("Turno reprogramado (#" + view.id() + ").");
            }
            refreshAppointments();
            selectAppointmentInTable(view.id());
            loadAppointment(view.id());
        } catch (ValidationException ex) {
            showAppointmentError(ex.getMessage());
        } catch (RuntimeException ex) {
            showAppointmentError("No se pudo guardar el turno: " + ex.getMessage());
        }
    }

    @FXML
    public void onCompleteAppointment() { mutateAppointment(nutritionService::completeAppointment, "Turno completado."); }
    @FXML
    public void onCancelAppointment() {
        if (!canManage || selectedAppointmentId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(cancelAppointmentButton, "Cancelar turno", "¿Cancelar este turno?",
                "El turno quedará marcado como cancelado.")) {
            return;
        }
        mutateAppointment(nutritionService::cancelAppointment, "Turno cancelado.");
    }
    @FXML
    public void onNoShowAppointment() { mutateAppointment(nutritionService::markNoShow, "Turno marcado como ausente."); }

    @FXML
    public void onNewPlan() {
        plansTable.getSelectionModel().clearSelection();
        selectedPlanId = null;
        planClientCombo.setDisable(false);
        planClientCombo.setValue(null);
        planTitleField.clear();
        planObjectivesField.clear();
        planMealGuidanceField.clear();
        planStatusCombo.setItems(FXCollections.observableArrayList(
                NutritionPlanStatus.DRAFT, NutritionPlanStatus.ACTIVE));
        planStatusCombo.setValue(NutritionPlanStatus.DRAFT);
        planStatusCombo.setDisable(!canManage);
        planValidFromPicker.setValue(LocalDate.now());
        planValidUntilPicker.setValue(null);
        planNotesField.clear();
        planSubtitleLabel.setText("Defina objetivos y guía alimentaria.");
        planStatusBadge.setText("Nuevo");
        planStatusBadge.getStyleClass().setAll("badge-ready");
        planStatusLabel.setText("");
        updatePlanActions(null);
    }

    @FXML
    public void onSavePlan() {
        if (!canManage) {
            return;
        }
        try {
            NutritionClientOption client = planClientCombo.getValue();
            if (client == null) {
                showPlanError("Seleccione un cliente.");
                return;
            }
            NutritionPlanRequest request = new NutritionPlanRequest(
                    client.id(),
                    planTitleField.getText(),
                    planObjectivesField.getText(),
                    planMealGuidanceField.getText(),
                    planStatusCombo.getValue(),
                    planValidFromPicker.getValue(),
                    planValidUntilPicker.getValue(),
                    planNotesField.getText());
            NutritionPlanView view = selectedPlanId == null
                    ? nutritionService.createPlan(request, sessionContext.requireUser().id())
                    : nutritionService.updatePlan(selectedPlanId, request);
            showPlanInfo(selectedPlanId == null ? "Plan creado (#" + view.id() + ")." : "Plan actualizado.");
            if (view.status() == NutritionPlanStatus.DRAFT && planScope == NutritionPlanListScope.ACTIVE) {
                setPlanScope(NutritionPlanListScope.DRAFT);
            } else if (view.status() == NutritionPlanStatus.ACTIVE && planScope == NutritionPlanListScope.DRAFT) {
                setPlanScope(NutritionPlanListScope.ACTIVE);
            } else {
                refreshPlans();
            }
            selectPlanInTable(view.id());
            loadPlan(view.id());
        } catch (ValidationException ex) {
            showPlanError(ex.getMessage());
        } catch (RuntimeException ex) {
            showPlanError("No se pudo guardar: " + ex.getMessage());
        }
    }

    @FXML
    public void onArchivePlan() {
        if (!canManage || selectedPlanId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(archivePlanButton, "Archivar plan", "¿Archivar este plan?",
                "Dejará de considerarse activo, pero se conserva en el historial.")) {
            return;
        }
        try {
            NutritionPlanView view = nutritionService.archivePlan(selectedPlanId);
            showPlanInfo("Plan archivado.");
            refreshPlans();
            loadPlan(view.id());
        } catch (RuntimeException ex) {
            showPlanError(ex.getMessage());
        }
    }

    @FXML
    public void onSaveHealthRecord() {
        if (!canManage) {
            return;
        }
        try {
            NutritionClientOption client = healthEntryClientCombo.getValue();
            if (client == null) {
                showHealthError("Seleccione un cliente para la nueva entrada.");
                return;
            }
            var view = nutritionService.addHealthRecord(
                    new HealthRecordRequest(
                            client.id(),
                            healthDatePicker.getValue(),
                            healthAllergiesField.getText(),
                            healthRestrictionsField.getText(),
                            healthConditionsField.getText(),
                            healthMedicationsField.getText(),
                            healthNotesField.getText()),
                    sessionContext.requireUser().id());
            showHealthInfo("Entrada agregada al historial (#" + view.id() + ").");
            clearHealthForm();
            healthEntryClientCombo.setValue(activeClients.stream()
                    .filter(c -> c.id().equals(view.clientId()))
                    .findFirst()
                    .orElse(healthEntryClientCombo.getValue()));
            if (!healthShowAll) {
                selectClient(healthClientCombo, view.clientId());
            }
            refreshHealthRecords();
        } catch (ValidationException ex) {
            showHealthError(ex.getMessage());
        } catch (RuntimeException ex) {
            showHealthError("No se pudo registrar: " + ex.getMessage());
        }
    }

    private void loadInitialData() {
        try {
            activeClients = nutritionService.listActiveClients();
            var items = FXCollections.observableArrayList(activeClients);
            appointmentClientCombo.setItems(items);
            planClientCombo.setItems(FXCollections.observableArrayList(activeClients));
            healthClientCombo.setItems(FXCollections.observableArrayList(activeClients));
            healthEntryClientCombo.setItems(FXCollections.observableArrayList(activeClients));
            refreshAppointments();
            refreshPlans();
            if (!activeClients.isEmpty()) {
                healthClientCombo.setValue(activeClients.get(0));
                healthEntryClientCombo.setValue(activeClients.get(0));
            }
            refreshHealthRecords();
        } catch (RuntimeException ex) {
            appointmentStatusLabel.setText("Error al cargar nutrición: " + ex.getMessage());
        }
    }

    private void setupTables() {
        appointmentsTable.setPlaceholder(new Label("No hay turnos para este filtro."));
        plansTable.setPlaceholder(new Label("No hay planes para este filtro."));
        healthTable.setPlaceholder(new Label("No hay entradas de ficha de salud."));

        appointmentDateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(DATE_TIME_FORMAT.format(data.getValue().scheduledAt())));
        appointmentClientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        appointmentStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(NutritionService.labelForAppointmentStatus(data.getValue().status())));
        appointmentStatusColumn.setCellFactory(col -> TableStatusCells.of((row, item) ->
                NutritionService.badgeClassForAppointmentStatus(row.status())));
        appointmentNutritionistColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().nutritionistName()));
        appointmentDetailColumn.setCellFactory(col -> appointmentDetailCell());
        appointmentDetailColumn.setSortable(false);

        planClientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        planTitleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        planValidityColumn.setCellValueFactory(data -> new SimpleStringProperty(formatValidity(data.getValue())));
        planStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(NutritionService.labelForPlanStatus(data.getValue().status())));
        planStatusColumn.setCellFactory(col -> TableStatusCells.of((row, item) ->
                NutritionService.badgeClassForPlanStatus(row.status())));
        planDetailColumn.setCellFactory(col -> planDetailCell());
        planDetailColumn.setSortable(false);

        healthDateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(DATE_FORMAT.format(data.getValue().recordedAt().toLocalDate())));
        healthClientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        healthAllergiesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().allergiesPreview()));
        healthRestrictionsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().restrictionsPreview()));
        healthAuthorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().authorName()));
        healthDetailColumn.setCellFactory(col -> healthDetailCell());
        healthDetailColumn.setSortable(false);
    }

    private TableCell<NutritionAppointmentSummary, String> appointmentDetailCell() {
        return new TableCell<>() {
            private final Button detail = new Button("ⓘ");
            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver detalle del turno"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < appointmentsTable.getItems().size()) {
                        openAppointmentDetail(appointmentsTable.getItems().get(getIndex()).id());
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= appointmentsTable.getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<NutritionPlanSummary, String> planDetailCell() {
        return new TableCell<>() {
            private final Button detail = new Button("ⓘ");
            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver detalle del plan"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < plansTable.getItems().size()) {
                        openPlanDetail(plansTable.getItems().get(getIndex()).id());
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= plansTable.getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<HealthRecordSummary, String> healthDetailCell() {
        return new TableCell<>() {
            private final Button detail = new Button("ⓘ");
            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver ficha completa"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < healthTable.getItems().size()) {
                        openHealthDetail(healthTable.getItems().get(getIndex()).id());
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= healthTable.getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private void setupCombos() {
        StringConverter<NutritionClientOption> clientConverter = new StringConverter<>() {
            @Override
            public String toString(NutritionClientOption option) {
                return option == null ? "" : formatClient(option);
            }
            @Override
            public NutritionClientOption fromString(String string) {
                return null;
            }
        };
        for (ComboBox<NutritionClientOption> combo : List.of(
                appointmentClientCombo, planClientCombo, healthClientCombo, healthEntryClientCombo)) {
            combo.setConverter(clientConverter);
            combo.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(NutritionClientOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : formatClient(item));
                }
            });
        }

        planStatusCombo.setItems(FXCollections.observableArrayList(
                NutritionPlanStatus.DRAFT, NutritionPlanStatus.ACTIVE));
        planStatusCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(NutritionPlanStatus status) {
                return status == null ? "" : NutritionService.labelForPlanStatus(status);
            }
            @Override
            public NutritionPlanStatus fromString(String string) {
                return null;
            }
        });
    }

    private void refreshAppointments() {
        List<NutritionAppointmentSummary> rows = nutritionService.listAppointments(
                appointmentSearchField.getText(), appointmentScope);
        appointmentsTable.setItems(FXCollections.observableArrayList(rows));
        appointmentsCountLabel.setText(rows.size() + (rows.size() == 1 ? " turno" : " turnos")
                + appointmentScopeLabel(appointmentScope));
    }

    private void refreshPlans() {
        List<NutritionPlanSummary> rows = nutritionService.listPlans(planSearchField.getText(), planScope);
        plansTable.setItems(FXCollections.observableArrayList(rows));
        plansCountLabel.setText(rows.size() + (rows.size() == 1 ? " plan" : " planes")
                + planScopeLabel(planScope));
    }

    private void refreshHealthRecords() {
        List<HealthRecordSummary> rows;
        if (healthShowAll) {
            rows = nutritionService.listAllHealthRecords(healthSearchField.getText());
            healthClientCombo.setVisible(false);
            healthClientCombo.setManaged(false);
            healthClientColumn.setVisible(true);
            healthCountLabel.setText(rows.size() + (rows.size() == 1 ? " entrada" : " entradas") + " · todos");
            healthSubtitleLabel.setText("Vista global. Use ⓘ para ver el detalle o agregue una entrada a la derecha.");
        } else {
            healthClientCombo.setVisible(true);
            healthClientCombo.setManaged(true);
            healthClientColumn.setVisible(false);
            NutritionClientOption client = healthClientCombo.getValue();
            if (client == null) {
                healthTable.setItems(FXCollections.observableArrayList());
                healthCountLabel.setText("0 entradas · elija un cliente");
                healthSubtitleLabel.setText("Seleccione un cliente para ver su historial o agregar una entrada.");
                return;
            }
            rows = nutritionService.listHealthRecords(client.id(), healthSearchField.getText());
            healthCountLabel.setText(rows.size() + (rows.size() == 1 ? " entrada" : " entradas")
                    + " · " + client.fullName());
            healthSubtitleLabel.setText("Historial de " + client.fullName() + ". Cada guardado suma una entrada nueva.");
            if (healthEntryClientCombo.getValue() == null
                    || !healthEntryClientCombo.getValue().id().equals(client.id())) {
                healthEntryClientCombo.setValue(client);
            }
        }
        healthTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void setHealthScope(boolean showAll) {
        healthShowAll = showAll;
        applyChipState(healthByClientButton, !showAll);
        applyChipState(healthAllButton, showAll);
        if (showAll) {
            healthClientCombo.setValue(null);
        } else if (healthClientCombo.getValue() == null && !activeClients.isEmpty()) {
            healthClientCombo.setValue(activeClients.get(0));
        }
        refreshHealthRecords();
    }

    private void loadAppointment(Long id) {
        try {
            NutritionAppointmentView view = nutritionService.getAppointment(id);
            selectedAppointmentId = view.id();
            selectClient(appointmentClientCombo, view.clientId());
            appointmentDatePicker.setValue(view.scheduledAt().toLocalDate());
            appointmentTimeField.setText(view.scheduledAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            appointmentNotesField.setText(view.notes() == null ? "" : view.notes());
            appointmentSubtitleLabel.setText(view.clientName() + " · " + view.nutritionistName());
            appointmentStatusBadge.setText(NutritionService.labelForAppointmentStatus(view.status()));
            appointmentStatusBadge.getStyleClass().setAll(NutritionService.badgeClassForAppointmentStatus(view.status()));
            updateAppointmentActions(view.status());
            appointmentStatusLabel.setText("");
        } catch (RuntimeException ex) {
            showAppointmentError(ex.getMessage());
        }
    }

    private void loadPlan(Long id) {
        try {
            NutritionPlanView view = nutritionService.getPlan(id);
            selectedPlanId = view.id();
            selectClient(planClientCombo, view.clientId());
            planClientCombo.setDisable(true);
            planTitleField.setText(view.title());
            planObjectivesField.setText(view.objectives() == null ? "" : view.objectives());
            planMealGuidanceField.setText(view.mealGuidance() == null ? "" : view.mealGuidance());
            if (view.status() == NutritionPlanStatus.ARCHIVED) {
                planStatusCombo.setItems(FXCollections.observableArrayList(NutritionPlanStatus.ARCHIVED));
                planStatusCombo.setValue(NutritionPlanStatus.ARCHIVED);
                planStatusCombo.setDisable(true);
            } else {
                planStatusCombo.setItems(FXCollections.observableArrayList(
                        NutritionPlanStatus.DRAFT, NutritionPlanStatus.ACTIVE));
                planStatusCombo.setValue(view.status());
                planStatusCombo.setDisable(!canManage);
            }
            planValidFromPicker.setValue(view.validFrom());
            planValidUntilPicker.setValue(view.validUntil());
            planNotesField.setText(view.notes() == null ? "" : view.notes());
            planSubtitleLabel.setText(view.clientName() + " · " + view.createdByName());
            planStatusBadge.setText(NutritionService.labelForPlanStatus(view.status()));
            planStatusBadge.getStyleClass().setAll(NutritionService.badgeClassForPlanStatus(view.status()));
            updatePlanActions(view.status());
            planStatusLabel.setText("");
        } catch (RuntimeException ex) {
            showPlanError(ex.getMessage());
        }
    }

    private void mutateAppointment(java.util.function.Function<Long, NutritionAppointmentView> action, String okMessage) {
        if (!canManage || selectedAppointmentId == null) {
            return;
        }
        try {
            NutritionAppointmentView view = action.apply(selectedAppointmentId);
            showAppointmentInfo(okMessage);
            refreshAppointments();
            loadAppointment(view.id());
        } catch (RuntimeException ex) {
            showAppointmentError(ex.getMessage());
        }
    }

    private void openHealthDetail(Long id) {
        try {
            Window owner = healthTable.getScene() == null ? null : healthTable.getScene().getWindow();
            HealthRecordDetailController.open(owner, nutritionService.getHealthRecord(id));
        } catch (RuntimeException ex) {
            showHealthError(ex.getMessage());
        }
    }

    private void openAppointmentDetail(Long id) {
        try {
            Window owner = appointmentsTable.getScene() == null ? null : appointmentsTable.getScene().getWindow();
            NutritionAppointmentDetailController.open(owner, nutritionService.getAppointment(id));
        } catch (RuntimeException ex) {
            showAppointmentError(ex.getMessage());
        }
    }

    private void openPlanDetail(Long id) {
        try {
            Window owner = plansTable.getScene() == null ? null : plansTable.getScene().getWindow();
            NutritionPlanDetailController.open(owner, nutritionService.getPlan(id));
        } catch (RuntimeException ex) {
            showPlanError(ex.getMessage());
        }
    }

    private void clearHealthForm() {
        healthDatePicker.setValue(LocalDate.now());
        healthAllergiesField.clear();
        healthRestrictionsField.clear();
        healthConditionsField.clear();
        healthMedicationsField.clear();
        healthNotesField.clear();
        healthStatusLabel.setText("");
    }

    private void setAppointmentScope(NutritionAppointmentListScope scope) {
        appointmentScope = scope;
        applyChipState(appointmentAllButton, scope == NutritionAppointmentListScope.ALL);
        applyChipState(appointmentUpcomingButton, scope == NutritionAppointmentListScope.UPCOMING);
        applyChipState(appointment30Button, scope == NutritionAppointmentListScope.LAST_30_DAYS);
        refreshAppointments();
    }

    private void setPlanScope(NutritionPlanListScope scope) {
        planScope = scope;
        applyChipState(planActiveButton, scope == NutritionPlanListScope.ACTIVE);
        applyChipState(planDraftButton, scope == NutritionPlanListScope.DRAFT);
        applyChipState(planArchivedButton, scope == NutritionPlanListScope.ARCHIVED);
        applyChipState(planAllButton, scope == NutritionPlanListScope.ALL);
        refreshPlans();
    }

    private void updateAppointmentActions(NutritionAppointmentStatus status) {
        boolean scheduled = status == NutritionAppointmentStatus.SCHEDULED;
        boolean isNew = selectedAppointmentId == null;
        boolean canEditSchedule = canManage && (isNew || scheduled);
        saveAppointmentButton.setDisable(!canEditSchedule);
        saveAppointmentButton.setText(isNew ? "Agendar" : "Guardar");
        completeAppointmentButton.setDisable(!canManage || !scheduled);
        cancelAppointmentButton.setDisable(!canManage || !scheduled);
        noShowAppointmentButton.setDisable(!canManage || !scheduled);
        viewAppointmentButton.setDisable(selectedAppointmentId == null);
        appointmentClientCombo.setDisable(!canManage || !isNew);
        appointmentDatePicker.setDisable(!canEditSchedule);
        appointmentTimeField.setDisable(!canEditSchedule);
        appointmentNotesField.setDisable(!canEditSchedule);
    }

    private void selectAppointmentInTable(Long id) {
        appointmentsTable.getItems().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .ifPresent(row -> appointmentsTable.getSelectionModel().select(row));
    }

    private void selectPlanInTable(Long id) {
        plansTable.getItems().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .ifPresent(row -> plansTable.getSelectionModel().select(row));
    }

    private void selectClient(ComboBox<NutritionClientOption> combo, Long clientId) {
        activeClients.stream()
                .filter(client -> client.id().equals(clientId))
                .findFirst()
                .ifPresent(combo::setValue);
    }

    private void updatePlanActions(NutritionPlanStatus status) {
        boolean isNew = selectedPlanId == null;
        boolean archived = status == NutritionPlanStatus.ARCHIVED;
        savePlanButton.setDisable(!canManage || archived);
        archivePlanButton.setDisable(!canManage || isNew || archived);
        viewPlanButton.setDisable(isNew);
        planTitleField.setDisable(!canManage || archived);
        planObjectivesField.setDisable(!canManage || archived);
        planMealGuidanceField.setDisable(!canManage || archived);
        planValidFromPicker.setDisable(!canManage || archived);
        planValidUntilPicker.setDisable(!canManage || archived);
        planNotesField.setDisable(!canManage || archived);
        if (isNew) {
            planClientCombo.setDisable(!canManage);
            planStatusCombo.setDisable(!canManage);
        }
    }

    private void applyPermissions() {
        newAppointmentButton.setDisable(!canManage);
        newPlanButton.setDisable(!canManage);
        savePlanButton.setDisable(!canManage);
        archivePlanButton.setDisable(true);
        saveHealthButton.setDisable(!canManage);
        healthEntryClientCombo.setDisable(!canManage);
        healthDatePicker.setDisable(!canManage);
        healthAllergiesField.setDisable(!canManage);
        healthRestrictionsField.setDisable(!canManage);
        healthConditionsField.setDisable(!canManage);
        healthMedicationsField.setDisable(!canManage);
        healthNotesField.setDisable(!canManage);
        if (!canManage) {
            healthStatusLabel.setText("Solo lectura: no tiene permiso para gestionar nutrición.");
        }
    }

    private static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Indique la hora del turno.");
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (Exception ex) {
            throw new ValidationException("La hora debe tener formato HH:mm.");
        }
    }

    private static String formatClient(NutritionClientOption option) {
        String number = option.clientNumber() == null ? "" : " · N° " + option.clientNumber();
        return option.fullName() + " (" + option.documentNumber() + ")" + number;
    }

    private static String formatValidity(NutritionPlanSummary summary) {
        String from = summary.validFrom() == null ? "—" : DATE_FORMAT.format(summary.validFrom());
        String until = summary.validUntil() == null ? "—" : DATE_FORMAT.format(summary.validUntil());
        return from + " → " + until;
    }

    private static String appointmentScopeLabel(NutritionAppointmentListScope scope) {
        return switch (scope) {
            case ALL -> "";
            case UPCOMING -> " · próximos";
            case LAST_30_DAYS -> " · últimos 30 días";
        };
    }

    private static String planScopeLabel(NutritionPlanListScope scope) {
        return switch (scope) {
            case ALL -> "";
            case ACTIVE -> " · activos";
            case DRAFT -> " · borradores";
            case ARCHIVED -> " · archivados";
        };
    }

    private static void applyChipState(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private void showAppointmentInfo(String message) { appointmentStatusLabel.setText(message); }
    private void showAppointmentError(String message) { appointmentStatusLabel.setText(message); }
    private void showPlanInfo(String message) { planStatusLabel.setText(message); }
    private void showPlanError(String message) { planStatusLabel.setText(message); }
    private void showHealthInfo(String message) { healthStatusLabel.setText(message); }
    private void showHealthError(String message) { healthStatusLabel.setText(message); }
}
