package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.TableStatusCells;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.checkin.dto.CheckInDetail;
import com.fitnesstraining.checkin.dto.CheckInEvaluation;
import com.fitnesstraining.checkin.dto.CheckInSnapshot;
import com.fitnesstraining.checkin.dto.CheckInSummary;
import com.fitnesstraining.checkin.dto.CheckInView;
import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.checkin.model.CheckInDenialReason;
import com.fitnesstraining.checkin.service.CheckInService;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CheckInController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private HBox feedbackBanner;
    @FXML private Label feedbackLabel;

    @FXML private Label entriesValueLabel;
    @FXML private Label entriesDetailLabel;
    @FXML private Label uniqueValueLabel;

    @FXML private TextField lookupField;
    @FXML private Button evaluateButton;
    @FXML private Label resultBadge;
    @FXML private Label resultMessageLabel;
    @FXML private Label clientNameLabel;
    @FXML private Label clientDocumentLabel;
    @FXML private Label accessModeLabel;
    @FXML private Label credentialLabel;
    @FXML private Label statusLabel;
    @FXML private Button registerButton;
    @FXML private Button openPaymentsButton;
    @FXML private Button clearButton;

    @FXML private Label todayCountLabel;
    @FXML private TableView<CheckInSummary> todayTable;
    @FXML private TableColumn<CheckInSummary, String> todayTimeColumn;
    @FXML private TableColumn<CheckInSummary, String> todayClientColumn;
    @FXML private TableColumn<CheckInSummary, String> todayDocumentColumn;
    @FXML private TableColumn<CheckInSummary, String> todayModeColumn;
    @FXML private TableColumn<CheckInSummary, String> todayCredentialColumn;
    @FXML private TableColumn<CheckInSummary, String> todayDetailColumn;

    @FXML private DatePicker historyDatePicker;
    @FXML private Label historyCountLabel;
    @FXML private TableView<CheckInSummary> historyTable;
    @FXML private TableColumn<CheckInSummary, String> historyTimeColumn;
    @FXML private TableColumn<CheckInSummary, String> historyClientColumn;
    @FXML private TableColumn<CheckInSummary, String> historyDocumentColumn;
    @FXML private TableColumn<CheckInSummary, String> historyModeColumn;
    @FXML private TableColumn<CheckInSummary, String> historyCredentialColumn;
    @FXML private TableColumn<CheckInSummary, String> historyDetailColumn;

    private final CheckInService checkInService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;
    private final AppContext appContext;

    private boolean canManage;
    private CheckInEvaluation currentEvaluation;
    private final PauseTransition feedbackHideDelay = new PauseTransition(Duration.seconds(4));

    public CheckInController(
            CheckInService checkInService,
            SessionContext sessionContext,
            AuthorizationService authorizationService,
            AppContext appContext) {
        this.checkInService = checkInService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.CHECKIN_MANAGE);
        applyPermissions();
        setupTables();

        lookupField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onEvaluate();
            }
        });
        historyDatePicker.setValue(LocalDate.now());
        feedbackHideDelay.setOnFinished(e -> hideFeedback());

        clearResult();
        Platform.runLater(() -> {
            onRefreshLists();
            lookupField.requestFocus();
        });
    }

    @FXML
    public void onDismissFeedback() {
        hideFeedback();
    }

    @FXML
    public void onEvaluate() {
        if (!canManage) {
            return;
        }
        try {
            currentEvaluation = checkInService.evaluate(lookupField.getText());
            showEvaluation(currentEvaluation);
            registerButton.setDisable(!currentEvaluation.allowed());
            if (currentEvaluation.allowed()) {
                statusInfo(currentEvaluation.alreadyCheckedInToday()
                        ? "Ya ingresó hoy. Puede registrar un reingreso."
                        : "Listo para registrar el ingreso.");
            } else {
                statusError(currentEvaluation.message());
            }
        } catch (ValidationException ex) {
            clearResult();
            statusError(ex.getMessage());
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            clearResult();
            statusError(ex.getMessage());
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onRegister() {
        if (!canManage || currentEvaluation == null || !currentEvaluation.allowed()) {
            return;
        }
        try {
            CheckInView view = checkInService.register(lookupField.getText());
            showFeedbackOk("Ingreso registrado: " + view.clientName());
            refreshSnapshot();
            refreshToday();
            selectTodayById(view.id());
            currentEvaluation = checkInService.evaluate(lookupField.getText());
            showEvaluation(currentEvaluation);
            registerButton.setDisable(!currentEvaluation.allowed());
            statusInfo("Ingreso registrado.");
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            statusError(ex.getMessage());
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onClear() {
        lookupField.clear();
        currentEvaluation = null;
        clearResult();
        lookupField.requestFocus();
    }

    @FXML
    public void onRefreshLists() {
        try {
            refreshSnapshot();
            refreshToday();
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onLoadHistory() {
        try {
            LocalDate day = historyDatePicker.getValue() == null ? LocalDate.now() : historyDatePicker.getValue();
            historyDatePicker.setValue(day);
            List<CheckInSummary> rows = checkInService.listByDate(day);
            historyTable.setItems(FXCollections.observableArrayList(rows));
            historyCountLabel.setText(rows.size() + (rows.size() == 1 ? " ingreso" : " ingresos")
                    + " · " + day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onOpenPayments() {
        if (currentEvaluation == null || currentEvaluation.clientId() == null) {
            return;
        }
        appContext.openPaymentsForClient(currentEvaluation.clientId());
    }

    private void setupTables() {
        bindSummaryColumns(todayTable, todayTimeColumn, todayClientColumn, todayDocumentColumn,
                todayModeColumn, todayCredentialColumn, todayDetailColumn);
        bindSummaryColumns(historyTable, historyTimeColumn, historyClientColumn, historyDocumentColumn,
                historyModeColumn, historyCredentialColumn, historyDetailColumn);
    }

    private void bindSummaryColumns(
            TableView<CheckInSummary> table,
            TableColumn<CheckInSummary, String> timeColumn,
            TableColumn<CheckInSummary, String> clientColumn,
            TableColumn<CheckInSummary, String> documentColumn,
            TableColumn<CheckInSummary, String> modeColumn,
            TableColumn<CheckInSummary, String> credentialColumn,
            TableColumn<CheckInSummary, String> detailColumn) {
        timeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().checkedInAt())));
        clientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        documentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientDocument()));
        modeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(labelForMode(data.getValue().accessMode())));
        modeColumn.setCellFactory(col -> TableStatusCells.of((row, item) ->
                row.accessMode() == AccessMode.MEMBERSHIP ? "badge-paid" : "badge-pending"));
        credentialColumn.setCellValueFactory(data ->
                new SimpleStringProperty(labelForCredentialType(data.getValue().credentialType())));
        detailColumn.setCellFactory(col -> detailActionCell(table));
        detailColumn.setSortable(false);
    }

    private TableCell<CheckInSummary, String> detailActionCell(TableView<CheckInSummary> table) {
        return new TableCell<>() {
            private final Button detail = new Button("ⓘ");

            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver detalle del ingreso"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < table.getItems().size()) {
                        openDetail(table.getItems().get(getIndex()).id());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= table.getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private void openDetail(Long checkInId) {
        try {
            CheckInDetail detail = checkInService.getDetail(checkInId);
            Window owner = todayTable.getScene() == null ? null : todayTable.getScene().getWindow();
            CheckInDetailController.open(owner, detail);
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    private void refreshSnapshot() {
        CheckInSnapshot snapshot = checkInService.snapshot();
        entriesValueLabel.setText(String.valueOf(snapshot.entriesToday()));
        entriesDetailLabel.setText("Registros del día");
        uniqueValueLabel.setText(String.valueOf(snapshot.uniqueClientsToday()));
    }

    private void refreshToday() {
        List<CheckInSummary> today = checkInService.listToday();
        todayTable.setItems(FXCollections.observableArrayList(today));
        todayCountLabel.setText(today.size() + (today.size() == 1 ? " ingreso" : " ingresos"));
    }

    private void selectTodayById(Long id) {
        todayTable.getItems().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .ifPresent(row -> todayTable.getSelectionModel().select(row));
    }

    private void showEvaluation(CheckInEvaluation evaluation) {
        resultMessageLabel.setText(evaluation.message());
        clientNameLabel.setText(blankToDash(evaluation.clientName()));
        clientDocumentLabel.setText(blankToDash(evaluation.clientDocument()));
        accessModeLabel.setText(evaluation.accessMode() == null
                ? "—"
                : labelForMode(evaluation.accessMode())
                + (evaluation.membershipPlanName() == null ? "" : " · " + evaluation.membershipPlanName()));
        credentialLabel.setText(formatCredential(evaluation.credentialType(), evaluation.credentialCode()));

        if (evaluation.allowed()) {
            resultBadge.setText(evaluation.alreadyCheckedInToday() ? "Reingreso" : "Permitido");
            resultBadge.getStyleClass().setAll("badge-paid");
            setOpenPaymentsVisible(false);
        } else {
            resultBadge.setText(labelForDenial(evaluation.denialReason()));
            resultBadge.getStyleClass().setAll(badgeForDenial(evaluation.denialReason()));
            setOpenPaymentsVisible(evaluation.denialReason() == CheckInDenialReason.OPEN_DEBT
                    && evaluation.clientId() != null);
        }
    }

    private void setOpenPaymentsVisible(boolean visible) {
        openPaymentsButton.setVisible(visible);
        openPaymentsButton.setManaged(visible);
    }

    private void clearResult() {
        clearResultPanels();
        resultBadge.setText("Esperando");
        resultBadge.getStyleClass().setAll("badge-pending");
        resultMessageLabel.setText("Escanee o escriba un identificador y pulse Verificar.");
        statusInfo("Recepción lista.");
        registerButton.setDisable(true);
        setOpenPaymentsVisible(false);
    }

    private void clearResultPanels() {
        clientNameLabel.setText("—");
        clientDocumentLabel.setText("—");
        accessModeLabel.setText("—");
        credentialLabel.setText("—");
    }

    private void applyPermissions() {
        evaluateButton.setDisable(!canManage);
        registerButton.setDisable(true);
        lookupField.setEditable(canManage);
        clearButton.setDisable(!canManage);
        if (!canManage) {
            statusInfo("Solo lectura: su rol no gestiona recepción.");
        }
    }

    private void statusInfo(String message) {
        statusLabel.getStyleClass().setAll("muted");
        statusLabel.setText(message == null ? "" : message);
    }

    private void statusError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message == null ? "" : message);
    }

    private void showFeedbackOk(String message) {
        feedbackBanner.getStyleClass().remove("error");
        feedbackLabel.setText(message);
        feedbackBanner.setVisible(true);
        feedbackBanner.setManaged(true);
        feedbackHideDelay.stop();
        feedbackHideDelay.playFromStart();
    }

    private void showFeedbackError(String message) {
        if (!feedbackBanner.getStyleClass().contains("error")) {
            feedbackBanner.getStyleClass().add("error");
        }
        feedbackLabel.setText(message);
        feedbackBanner.setVisible(true);
        feedbackBanner.setManaged(true);
        feedbackHideDelay.stop();
    }

    private void hideFeedback() {
        feedbackHideDelay.stop();
        feedbackBanner.setVisible(false);
        feedbackBanner.setManaged(false);
        feedbackBanner.getStyleClass().remove("error");
        feedbackLabel.setText("");
    }

    private static String formatTime(OffsetDateTime value) {
        return value == null ? "—" : TIME_FORMAT.format(value.toLocalTime());
    }

    private static String labelForMode(AccessMode mode) {
        if (mode == null) {
            return "—";
        }
        return switch (mode) {
            case MEMBERSHIP -> "Membresía";
            case DAILY_PASS -> "Pase diario";
        };
    }

    private static String labelForCredentialType(CredentialType type) {
        if (type == null) {
            return "Doc.";
        }
        return switch (type) {
            case CLIENT_NUMBER -> "N° cliente";
            case CARD -> "Carnet";
            case QR -> "QR";
        };
    }

    private static String formatCredential(CredentialType type, String code) {
        if (code == null || code.isBlank()) {
            return type == null ? "Documento" : labelForCredentialType(type);
        }
        return labelForCredentialType(type) + " · " + code;
    }

    private static String labelForDenial(CheckInDenialReason reason) {
        if (reason == null) {
            return "Denegado";
        }
        return switch (reason) {
            case NOT_FOUND -> "No encontrado";
            case CLIENT_INACTIVE -> "Inactivo";
            case CREDENTIAL_EXPIRED -> "Credencial vencida";
            case OPEN_DEBT -> "En mora";
            case NO_ACCESS -> "Sin acceso";
        };
    }

    private static String badgeForDenial(CheckInDenialReason reason) {
        if (reason == CheckInDenialReason.OPEN_DEBT) {
            return "badge-overdue";
        }
        if (reason == CheckInDenialReason.NO_ACCESS || reason == CheckInDenialReason.CREDENTIAL_EXPIRED) {
            return "badge-pending";
        }
        return "badge-cancelled";
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
