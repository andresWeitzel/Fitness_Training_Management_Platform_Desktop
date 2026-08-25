package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
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
import com.fitnesstraining.members.dto.CredentialView;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CheckInController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
    @FXML private Label todayDetailBadge;
    @FXML private Label todayDetailSummaryLabel;
    @FXML private Label todayDetailClientLabel;
    @FXML private Label todayDetailContactLabel;
    @FXML private Label todayDetailModeLabel;
    @FXML private Label todayDetailUsedLabel;
    @FXML private VBox todayCredentialsBox;

    @FXML private DatePicker historyDatePicker;
    @FXML private Label historyCountLabel;
    @FXML private TableView<CheckInSummary> historyTable;
    @FXML private TableColumn<CheckInSummary, String> historyTimeColumn;
    @FXML private TableColumn<CheckInSummary, String> historyClientColumn;
    @FXML private TableColumn<CheckInSummary, String> historyDocumentColumn;
    @FXML private TableColumn<CheckInSummary, String> historyModeColumn;
    @FXML private TableColumn<CheckInSummary, String> historyCredentialColumn;
    @FXML private Label historyDetailBadge;
    @FXML private Label historyDetailSummaryLabel;
    @FXML private Label historyDetailClientLabel;
    @FXML private Label historyDetailContactLabel;
    @FXML private Label historyDetailModeLabel;
    @FXML private Label historyDetailUsedLabel;
    @FXML private VBox historyCredentialsBox;

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
        todayTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadDetail(selected.id(), true);
            }
        });
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadDetail(selected.id(), false);
            }
        });
        historyDatePicker.setValue(LocalDate.now());
        feedbackHideDelay.setOnFinished(e -> hideFeedback());

        clearResult();
        clearDetail(true);
        clearDetail(false);
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
                        ? "Pulse Registrar ingreso para confirmar el reingreso."
                        : "Pulse Registrar ingreso para confirmar.");
            } else {
                statusError(currentEvaluation.message());
            }
        } catch (ValidationException ex) {
            currentEvaluation = null;
            clearResultPanels();
            resultBadge.setText("Error");
            resultBadge.getStyleClass().setAll("badge-cancelled");
            resultMessageLabel.setText(ex.getMessage());
            statusError(ex.getMessage());
            registerButton.setDisable(true);
        } catch (RuntimeException ex) {
            currentEvaluation = null;
            showFeedbackError("No se pudo verificar: " + ex.getMessage());
        }
    }

    @FXML
    public void onRegister() {
        if (!canManage) {
            return;
        }
        try {
            CheckInView view = checkInService.register(lookupField.getText());
            showFeedbackOk(view.message() + " " + view.clientName());
            onRefreshLists();
            currentEvaluation = checkInService.evaluate(lookupField.getText());
            showEvaluation(currentEvaluation);
            registerButton.setDisable(!currentEvaluation.allowed());
            statusInfo(view.message());
            selectTodayById(view.id());
            lookupField.selectAll();
            lookupField.requestFocus();
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
            onEvaluate();
        } catch (RuntimeException ex) {
            showFeedbackError("No se pudo registrar el ingreso: " + ex.getMessage());
        }
    }

    @FXML
    public void onClear() {
        lookupField.clear();
        currentEvaluation = null;
        clearResult();
        refreshSnapshot();
        lookupField.requestFocus();
    }

    @FXML
    public void onRefreshLists() {
        refreshSnapshot();
        refreshToday();
        if (historyDatePicker.getValue() != null) {
            onLoadHistory();
        }
    }

    @FXML
    public void onLoadHistory() {
        LocalDate date = historyDatePicker.getValue();
        if (date == null) {
            statusError("Seleccione una fecha para el histórico.");
            return;
        }
        try {
            List<CheckInSummary> rows = checkInService.listByDate(date);
            historyTable.setItems(FXCollections.observableArrayList(rows));
            historyCountLabel.setText(rows.size() + (rows.size() == 1 ? " ingreso" : " ingresos")
                    + " · " + DATE_FORMAT.format(date));
            clearDetail(false);
        } catch (RuntimeException ex) {
            showFeedbackError("No se pudo cargar el histórico: " + ex.getMessage());
        }
    }

    private void setupTables() {
        bindSummaryColumns(todayTable, todayTimeColumn, todayClientColumn, todayDocumentColumn,
                todayModeColumn, todayCredentialColumn);
        bindSummaryColumns(historyTable, historyTimeColumn, historyClientColumn, historyDocumentColumn,
                historyModeColumn, historyCredentialColumn);
    }

    private void bindSummaryColumns(
            TableView<CheckInSummary> table,
            TableColumn<CheckInSummary, String> timeColumn,
            TableColumn<CheckInSummary, String> clientColumn,
            TableColumn<CheckInSummary, String> documentColumn,
            TableColumn<CheckInSummary, String> modeColumn,
            TableColumn<CheckInSummary, String> credentialColumn) {
        timeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().checkedInAt())));
        clientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        documentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientDocument()));
        modeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(labelForMode(data.getValue().accessMode())));
        modeColumn.setCellFactory(col -> modeBadgeCell());
        credentialColumn.setCellValueFactory(data ->
                new SimpleStringProperty(labelForCredentialType(data.getValue().credentialType())));
    }

    private TableCell<CheckInSummary, String> modeBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableView() == null
                        || getIndex() < 0
                        || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                CheckInSummary row = getTableView().getItems().get(getIndex());
                Label badge = new Label(item);
                badge.getStyleClass().setAll(
                        "table-status-badge",
                        row.accessMode() == AccessMode.MEMBERSHIP ? "badge-paid" : "badge-pending");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        };
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
                .ifPresent(row -> {
                    todayTable.getSelectionModel().select(row);
                    loadDetail(id, true);
                });
    }

    private void loadDetail(Long checkInId, boolean todayPanel) {
        try {
            CheckInDetail detail = checkInService.getDetail(checkInId);
            applyDetail(detail, todayPanel);
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    private void applyDetail(CheckInDetail detail, boolean todayPanel) {
        Label badge = todayPanel ? todayDetailBadge : historyDetailBadge;
        Label summary = todayPanel ? todayDetailSummaryLabel : historyDetailSummaryLabel;
        Label client = todayPanel ? todayDetailClientLabel : historyDetailClientLabel;
        Label contact = todayPanel ? todayDetailContactLabel : historyDetailContactLabel;
        Label mode = todayPanel ? todayDetailModeLabel : historyDetailModeLabel;
        Label used = todayPanel ? todayDetailUsedLabel : historyDetailUsedLabel;
        VBox credentialsBox = todayPanel ? todayCredentialsBox : historyCredentialsBox;

        badge.setText(labelForMode(detail.accessMode()));
        badge.getStyleClass().setAll(
                detail.accessMode() == AccessMode.MEMBERSHIP ? "badge-paid" : "badge-pending");
        summary.setText("Ingreso #" + detail.checkInId() + " · "
                + formatDateTime(detail.checkedInAt())
                + (detail.notes() == null || detail.notes().isBlank() ? "" : " · " + detail.notes()));
        client.setText(detail.clientName() + " · " + detail.clientDocument()
                + (detail.clientNumber() == null ? "" : " · " + detail.clientNumber()));
        contact.setText(joinContact(detail.clientEmail(), detail.clientPhone()));
        mode.setText(labelForMode(detail.accessMode())
                + (detail.membershipPlanName() == null ? "" : " · " + detail.membershipPlanName()));
        used.setText(formatCredential(detail.usedCredentialType(), detail.usedCredentialCode()));
        renderCredentials(credentialsBox, detail.credentials());
    }

    private void renderCredentials(VBox box, List<CredentialView> credentials) {
        box.getChildren().clear();
        if (credentials == null || credentials.isEmpty()) {
            Label empty = new Label("Sin credenciales emitidas.");
            empty.getStyleClass().add("muted");
            box.getChildren().add(empty);
            return;
        }
        for (CredentialView credential : credentials) {
            box.getChildren().add(credentialCard(credential));
        }
    }

    private HBox credentialCard(CredentialView credential) {
        Label type = new Label(credential.typeLabel());
        type.getStyleClass().add("credential-type");
        Label code = new Label(credential.code());
        code.getStyleClass().add("credential-code");
        code.setOnMouseClicked(event -> copyCredential(credential));
        Label meta = new Label(credential.expiresAt() == null
                ? "Sin vencimiento"
                : "Vence " + DATE_FORMAT.format(credential.expiresAt().toLocalDate()));
        meta.getStyleClass().add("muted");
        VBox text = new VBox(2, type, code, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label status = new Label(credential.statusLabel());
        status.getStyleClass().add("VIGENTE".equals(credential.statusLabel()) ? "badge-ready" : "badge-soon");
        Button copyButton = new Button("Copiar");
        copyButton.getStyleClass().add("credential-copy-button");
        copyButton.setOnAction(event -> copyCredential(credential));
        HBox row = new HBox(10, text, spacer, status, copyButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("credential-card");
        return row;
    }

    private void copyCredential(CredentialView credential) {
        ClipboardContent content = new ClipboardContent();
        content.putString(credential.code());
        Clipboard.getSystemClipboard().setContent(content);
        showFeedbackOk(credential.typeLabel() + " copiado: " + credential.code());
    }

    private void clearDetail(boolean todayPanel) {
        Label badge = todayPanel ? todayDetailBadge : historyDetailBadge;
        Label summary = todayPanel ? todayDetailSummaryLabel : historyDetailSummaryLabel;
        Label client = todayPanel ? todayDetailClientLabel : historyDetailClientLabel;
        Label contact = todayPanel ? todayDetailContactLabel : historyDetailContactLabel;
        Label mode = todayPanel ? todayDetailModeLabel : historyDetailModeLabel;
        Label used = todayPanel ? todayDetailUsedLabel : historyDetailUsedLabel;
        VBox credentialsBox = todayPanel ? todayCredentialsBox : historyCredentialsBox;

        badge.setText("Sin selección");
        badge.getStyleClass().setAll("badge-pending");
        summary.setText("Seleccione un ingreso de la lista para ver el detalle del cliente y sus credenciales.");
        client.setText("—");
        contact.setText("—");
        mode.setText("—");
        used.setText("—");
        credentialsBox.getChildren().clear();
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

    @FXML
    public void onOpenPayments() {
        if (currentEvaluation == null || currentEvaluation.clientId() == null) {
            return;
        }
        appContext.openPaymentsForClient(currentEvaluation.clientId());
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

    private static String formatDateTime(OffsetDateTime value) {
        return value == null ? "—" : DATE_TIME_FORMAT.format(value);
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

    private static String joinContact(String email, String phone) {
        StringBuilder builder = new StringBuilder();
        if (email != null && !email.isBlank()) {
            builder.append(email);
        }
        if (phone != null && !phone.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" · ");
            }
            builder.append(phone);
        }
        return builder.isEmpty() ? "—" : builder.toString();
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
