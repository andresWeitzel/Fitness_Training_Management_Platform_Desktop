package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.ConfirmDialogs;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.payments.dto.PaymentClientOption;
import com.fitnesstraining.payments.dto.PaymentMembershipOption;
import com.fitnesstraining.payments.dto.PaymentSummary;
import com.fitnesstraining.payments.dto.PaymentView;
import com.fitnesstraining.payments.dto.RegisterPaymentRequest;
import com.fitnesstraining.payments.model.PaymentListScope;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.payments.service.PaymentService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PaymentsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @FXML private HBox feedbackBanner;
    @FXML private Label feedbackLabel;

    @FXML private Label paymentsCountLabel;
    @FXML private Button filterAllButton;
    @FXML private Button filterPaidButton;
    @FXML private Button filterPendingButton;
    @FXML private Button filterOverdueButton;
    @FXML private Button filterCancelledButton;
    @FXML private TextField searchField;
    @FXML private Button newPaymentButton;
    @FXML private TableView<PaymentSummary> paymentsTable;
    @FXML private TableColumn<PaymentSummary, String> clientColumn;
    @FXML private TableColumn<PaymentSummary, String> typeColumn;
    @FXML private TableColumn<PaymentSummary, String> amountColumn;
    @FXML private TableColumn<PaymentSummary, String> dateColumn;
    @FXML private TableColumn<PaymentSummary, String> statusColumn;

    @FXML private Label subtitleLabel;
    @FXML private Label statusBadge;
    @FXML private ComboBox<PaymentClientOption> clientCombo;
    @FXML private ComboBox<PaymentType> typeCombo;
    @FXML private ComboBox<PaymentMembershipOption> membershipCombo;
    @FXML private TextField amountField;
    @FXML private ComboBox<PaymentMethod> methodCombo;
    @FXML private DatePicker dueDatePicker;
    @FXML private CheckBox markAsPaidCheck;
    @FXML private TextArea notesField;
    @FXML private Label previewTypeLabel;
    @FXML private Label previewAmountLabel;
    @FXML private Label previewDateLabel;
    @FXML private Label previewMethodLabel;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;
    @FXML private Button markPaidButton;
    @FXML private Button cancelButton;

    private final PaymentService paymentService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;
    private final AppContext appContext;

    private Long selectedPaymentId;
    private PaymentStatus selectedStatus;
    private boolean canManage;
    private boolean suggestingAmount;
    private PaymentListScope scope = PaymentListScope.ALL;
    private List<PaymentClientOption> payableClients = List.of();
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition feedbackHideDelay = new PauseTransition(Duration.seconds(4));

    public PaymentsController(
            PaymentService paymentService,
            SessionContext sessionContext,
            AuthorizationService authorizationService,
            AppContext appContext) {
        this.paymentService = paymentService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.PAYMENTS_MANAGE);
        applyPermissions();

        setupTable();
        setupCombos();
        bindFieldErrorClearing();

        searchDelay.setOnFinished(e -> refreshPayments());
        searchField.textProperty().addListener((obs, old, value) -> {
            searchDelay.stop();
            searchDelay.playFromStart();
        });

        paymentsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadPayment(selected.id());
            }
        });

        clientCombo.valueProperty().addListener((obs, old, client) -> {
            refreshMembershipOptions();
            maybeSuggestAmount();
            updatePreview();
        });
        typeCombo.valueProperty().addListener((obs, old, type) -> {
            refreshMembershipOptions();
            maybeSuggestAmount();
            updatePreview();
        });
        membershipCombo.valueProperty().addListener((obs, old, membership) -> {
            maybeSuggestAmount();
            updatePreview();
        });
        methodCombo.valueProperty().addListener((obs, old, method) -> updatePreview());
        amountField.textProperty().addListener((obs, old, value) -> updatePreview());
        dueDatePicker.valueProperty().addListener((obs, old, value) -> updatePreview());
        markAsPaidCheck.selectedProperty().addListener((obs, old, value) -> updatePreview());

        feedbackHideDelay.setOnFinished(e -> hideFeedback());

        onNewPayment();
        Platform.runLater(this::loadInitialData);
    }

    private void loadInitialData() {
        try {
            refreshPayableClients();
            Long pendingClientId = appContext.consumePendingPaymentClientId().orElse(null);
            if (pendingClientId != null) {
                focusClientFromCheckIn(pendingClientId);
            } else {
                refreshPayments();
            }
        } catch (RuntimeException ex) {
            showFeedbackError("Error al cargar pagos: " + ex.getMessage());
        }
    }

    private void focusClientFromCheckIn(Long clientId) {
        setScope(PaymentListScope.OVERDUE);
        PaymentClientOption client = payableClients.stream()
                .filter(c -> c.id().equals(clientId))
                .findFirst()
                .orElse(null);
        if (client != null) {
            searchField.setText(client.documentNumber());
            onNewPayment();
            clientCombo.setValue(client);
            showFeedbackOk("Cliente precargado desde Recepción (mora). Registre o marque el cobro.");
        } else {
            refreshPayments();
            showFeedbackError("No se encontró el cliente indicado desde Recepción.");
        }
    }

    @FXML
    public void onDismissFeedback() {
        hideFeedback();
    }

    @FXML
    public void onFilterAll() {
        setScope(PaymentListScope.ALL);
    }

    @FXML
    public void onFilterPaid() {
        setScope(PaymentListScope.PAID);
    }

    @FXML
    public void onFilterPending() {
        setScope(PaymentListScope.PENDING);
    }

    @FXML
    public void onFilterOverdue() {
        setScope(PaymentListScope.OVERDUE);
    }

    @FXML
    public void onFilterCancelled() {
        setScope(PaymentListScope.CANCELLED);
    }

    @FXML
    public void onNewPayment() {
        paymentsTable.getSelectionModel().clearSelection();
        selectedPaymentId = null;
        selectedStatus = null;
        clearForm();
        applyFormMode();
        updatePreview();
    }

    @FXML
    public void onSavePayment() {
        if (!canManage) {
            return;
        }
        if (selectedPaymentId != null) {
            showFormError("Seleccione Nuevo cobro para registrar un pago.", null);
            return;
        }
        try {
            clearFieldErrors();
            PaymentView view = paymentService.register(buildRequest());
            showFeedbackOk("Pago registrado (#" + view.id() + ").");
            refreshPayments();
            selectPaymentInTable(view.id());
            loadPayment(view.id());
        } catch (ValidationException ex) {
            showFormError(ex.getMessage(), fieldForMessage(ex.getMessage()));
        } catch (RuntimeException ex) {
            showFormError("No se pudo registrar el pago: " + ex.getMessage(), null);
        }
    }

    @FXML
    public void onMarkPaid() {
        if (!canManage || selectedPaymentId == null) {
            return;
        }
        clearFieldErrors();
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            showFormError("Seleccione el medio de pago.", methodCombo);
            methodCombo.requestFocus();
            return;
        }
        if (!ConfirmDialogs.confirm(
                markPaidButton,
                "Cobrar pago",
                "¿Marcar este pago como cobrado?",
                "Se registrará el cobro con el medio seleccionado y se limpiará la deuda pendiente.")) {
            return;
        }
        try {
            PaymentView view = paymentService.markPaid(selectedPaymentId, method);
            showFeedbackOk("Pago cobrado (#" + view.id() + ").");
            refreshPayments();
            loadPayment(view.id());
        } catch (ValidationException ex) {
            showFormError(ex.getMessage(), fieldForMessage(ex.getMessage()));
        } catch (RuntimeException ex) {
            showFormError("No se pudo cobrar: " + ex.getMessage(), null);
        }
    }

    @FXML
    public void onCancelPayment() {
        if (!canManage || selectedPaymentId == null) {
            return;
        }
        clearFieldErrors();
        if (!ConfirmDialogs.confirm(
                cancelButton,
                "Cancelar pago",
                "¿Cancelar este pago pendiente?",
                "El cobro dejará de generar deuda. Esta acción no se puede deshacer desde aquí.")) {
            return;
        }
        try {
            PaymentView view = paymentService.cancel(selectedPaymentId);
            showFeedbackOk("Pago cancelado (#" + view.id() + ").");
            refreshPayments();
            loadPayment(view.id());
        } catch (ValidationException ex) {
            showFormError(ex.getMessage(), null);
        } catch (RuntimeException ex) {
            showFormError("No se pudo cancelar: " + ex.getMessage(), null);
        }
    }

    private void setupTable() {
        clientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(labelForType(data.getValue().type())));
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().amount())));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(formatPrimaryDate(data.getValue())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(labelForStatus(data.getValue())));
        statusColumn.setCellFactory(col -> new TableCell<>() {
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
                PaymentSummary row = getTableView().getItems().get(getIndex());
                Label badge = new Label(item);
                badge.getStyleClass().setAll(
                        "table-status-badge",
                        badgeClassForStatus(row.status(), row.overdue()));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private void setupCombos() {
        typeCombo.setItems(FXCollections.observableArrayList(PaymentType.values()));
        typeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentType type) {
                return type == null ? "" : labelForType(type);
            }

            @Override
            public PaymentType fromString(String string) {
                return null;
            }
        });
        typeCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PaymentType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labelForType(item));
            }
        });

        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMethod method) {
                return method == null ? "" : labelForMethod(method);
            }

            @Override
            public PaymentMethod fromString(String string) {
                return null;
            }
        });
        methodCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PaymentMethod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labelForMethod(item));
            }
        });

        clientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentClientOption option) {
                return option == null ? "" : formatClient(option);
            }

            @Override
            public PaymentClientOption fromString(String string) {
                return null;
            }
        });
        clientCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PaymentClientOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatClient(item));
            }
        });

        membershipCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMembershipOption option) {
                return option == null ? "" : formatMembership(option);
            }

            @Override
            public PaymentMembershipOption fromString(String string) {
                return null;
            }
        });
        membershipCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PaymentMembershipOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatMembership(item));
            }
        });
    }

    private void refreshPayments() {
        List<PaymentSummary> payments = paymentService.list(searchField.getText(), scope);
        paymentsTable.setItems(FXCollections.observableArrayList(payments));
        paymentsCountLabel.setText(payments.size() + (payments.size() == 1 ? " pago" : " pagos"));
    }

    private void refreshPayableClients() {
        payableClients = paymentService.listPayableClients();
        clientCombo.setItems(FXCollections.observableArrayList(payableClients));
    }

    private void refreshMembershipOptions() {
        PaymentClientOption client = clientCombo.getValue();
        PaymentType type = typeCombo.getValue();
        if (client == null || type == PaymentType.DAILY_PASS) {
            membershipCombo.setItems(FXCollections.observableArrayList());
            membershipCombo.setValue(null);
            membershipCombo.setDisable(true);
            return;
        }
        List<PaymentMembershipOption> options = paymentService.listMembershipOptions(client.id());
        membershipCombo.setItems(FXCollections.observableArrayList(options));
        membershipCombo.setDisable(!canManage || selectedPaymentId != null);
        if (options.size() == 1) {
            membershipCombo.setValue(options.get(0));
        }
    }

    private void loadPayment(Long id) {
        try {
            PaymentView view = paymentService.get(id);
            selectedPaymentId = view.id();
            selectedStatus = view.status();
            fillForm(view);
            applyFormMode();
            updatePreview();
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    private void fillForm(PaymentView view) {
        PaymentClientOption client = payableClients.stream()
                .filter(c -> c.id().equals(view.clientId()))
                .findFirst()
                .orElse(new PaymentClientOption(view.clientId(), view.clientDocument(), view.clientName(), null));
        if (clientCombo.getItems().stream().noneMatch(c -> c.id().equals(client.id()))) {
            clientCombo.getItems().add(client);
        }
        clientCombo.setValue(client);
        typeCombo.setValue(view.type());
        refreshMembershipOptions();
        if (view.clientMembershipId() != null) {
            membershipCombo.getItems().stream()
                    .filter(m -> m.id().equals(view.clientMembershipId()))
                    .findFirst()
                    .ifPresentOrElse(
                            membershipCombo::setValue,
                            () -> membershipCombo.setValue(new PaymentMembershipOption(
                                    view.clientMembershipId(),
                                    view.membershipPlanName() == null ? "Membresía" : view.membershipPlanName(),
                                    view.amount(),
                                    "")));
        } else {
            membershipCombo.setValue(null);
        }
        amountField.setText(view.amount().toPlainString());
        methodCombo.setValue(view.method());
        dueDatePicker.setValue(view.dueAt() == null ? null : view.dueAt().toLocalDate());
        markAsPaidCheck.setSelected(view.status() == PaymentStatus.PAID);
        notesField.setText(view.notes() == null ? "" : view.notes());
        subtitleLabel.setText(view.clientName() + " · #" + view.id());
        applyStatusBadge(view);
        clearFieldErrors();
        statusInfo(statusHelp(view));
    }

    private void clearForm() {
        clearFieldErrors();
        clientCombo.setValue(null);
        typeCombo.setValue(PaymentType.MEMBERSHIP);
        membershipCombo.setItems(FXCollections.observableArrayList());
        membershipCombo.setValue(null);
        amountField.clear();
        methodCombo.setValue(PaymentMethod.CASH);
        dueDatePicker.setValue(null);
        markAsPaidCheck.setSelected(true);
        notesField.clear();
        subtitleLabel.setText("Nuevo cobro");
        statusBadge.setText("Nuevo");
        statusBadge.getStyleClass().setAll("badge-ready");
        statusInfo("Complete cliente, tipo y monto. Puede dejarlo pendiente o cobrado.");
        previewTypeLabel.setText("—");
        previewAmountLabel.setText("—");
        previewDateLabel.setText("—");
        previewMethodLabel.setText("—");
    }

    private void applyFormMode() {
        boolean creating = selectedPaymentId == null;
        boolean pending = selectedStatus == PaymentStatus.PENDING;
        clientCombo.setDisable(!canManage || !creating);
        typeCombo.setDisable(!canManage || !creating);
        membershipCombo.setDisable(!canManage || !creating || typeCombo.getValue() == PaymentType.DAILY_PASS);
        amountField.setEditable(canManage && creating);
        methodCombo.setDisable(!canManage || (!creating && !pending));
        dueDatePicker.setDisable(!canManage || !creating);
        markAsPaidCheck.setDisable(!canManage || !creating);
        notesField.setEditable(canManage && creating);
        saveButton.setDisable(!canManage || !creating);
        markPaidButton.setDisable(!canManage || !pending);
        cancelButton.setDisable(!canManage || !pending);
        saveButton.setText(creating ? "Registrar cobro" : "Solo lectura");
    }

    private void applyPermissions() {
        newPaymentButton.setDisable(!canManage);
        saveButton.setDisable(!canManage);
        markPaidButton.setDisable(!canManage);
        cancelButton.setDisable(!canManage);
        clientCombo.setDisable(!canManage);
        typeCombo.setDisable(!canManage);
        membershipCombo.setDisable(!canManage);
        amountField.setEditable(canManage);
        methodCombo.setDisable(!canManage);
        dueDatePicker.setDisable(!canManage);
        markAsPaidCheck.setDisable(!canManage);
        notesField.setEditable(canManage);
    }

    private RegisterPaymentRequest buildRequest() {
        BigDecimal amount;
        try {
            String raw = amountField.getText() == null ? "" : amountField.getText().trim().replace(',', '.');
            amount = new BigDecimal(raw.isEmpty() ? "0" : raw);
        } catch (NumberFormatException ex) {
            throw new ValidationException("El monto no es válido.");
        }
        PaymentClientOption client = clientCombo.getValue();
        PaymentMembershipOption membership = membershipCombo.getValue();
        return new RegisterPaymentRequest(
                client == null ? null : client.id(),
                membership == null ? null : membership.id(),
                typeCombo.getValue(),
                amount,
                methodCombo.getValue(),
                dueDatePicker.getValue(),
                markAsPaidCheck.isSelected(),
                notesField.getText());
    }

    private void maybeSuggestAmount() {
        if (selectedPaymentId != null || suggestingAmount) {
            return;
        }
        PaymentType type = typeCombo.getValue();
        if (type == null) {
            return;
        }
        PaymentMembershipOption membership = membershipCombo.getValue();
        BigDecimal suggested = paymentService.suggestAmount(
                type,
                membership == null ? null : membership.id());
        if (suggested.compareTo(BigDecimal.ZERO) <= 0 && type != PaymentType.LATE_FEE) {
            return;
        }
        suggestingAmount = true;
        try {
            if (suggested.compareTo(BigDecimal.ZERO) > 0) {
                amountField.setText(suggested.toPlainString());
            }
        } finally {
            suggestingAmount = false;
        }
    }

    private void updatePreview() {
        PaymentType type = typeCombo.getValue();
        previewTypeLabel.setText(type == null ? "—" : labelForType(type));
        try {
            String raw = amountField.getText() == null ? "" : amountField.getText().trim().replace(',', '.');
            previewAmountLabel.setText(raw.isEmpty() ? "—" : formatMoney(new BigDecimal(raw)));
        } catch (NumberFormatException ex) {
            previewAmountLabel.setText("—");
        }
        if (markAsPaidCheck.isSelected() || selectedStatus == PaymentStatus.PAID) {
            previewDateLabel.setText("Hoy / cobrado");
        } else if (dueDatePicker.getValue() != null) {
            previewDateLabel.setText("Vence " + formatDate(dueDatePicker.getValue()));
        } else {
            previewDateLabel.setText("Pendiente");
        }
        PaymentMethod method = methodCombo.getValue();
        previewMethodLabel.setText(method == null ? "—" : labelForMethod(method));
    }

    private void setScope(PaymentListScope next) {
        scope = next;
        applyChipState(filterAllButton, next == PaymentListScope.ALL);
        applyChipState(filterPaidButton, next == PaymentListScope.PAID);
        applyChipState(filterPendingButton, next == PaymentListScope.PENDING);
        applyChipState(filterOverdueButton, next == PaymentListScope.OVERDUE);
        applyChipState(filterCancelledButton, next == PaymentListScope.CANCELLED);
        refreshPayments();
    }

    private void selectPaymentInTable(Long id) {
        paymentsTable.getItems().stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .ifPresent(p -> paymentsTable.getSelectionModel().select(p));
    }

    private void applyStatusBadge(PaymentView view) {
        statusBadge.setText(labelForStatus(view.status(), view.overdue()));
        statusBadge.getStyleClass().setAll(badgeClassForStatus(view.status(), view.overdue()));
    }

    private static String statusHelp(PaymentView view) {
        if (view.overdue()) {
            return "En mora: hay un cobro pendiente vencido. Al marcarlo cobrado se limpia la deuda.";
        }
        return switch (view.status()) {
            case PAID -> "Cobrado el " + formatDateTime(view.paidAt()) + ".";
            case PENDING -> "Pendiente de cobro. Puede marcarlo cobrado o cancelarlo.";
            case CANCELLED -> "Cancelado. No genera deuda para el check-in.";
        };
    }

    private static void applyChipState(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private static String formatClient(PaymentClientOption option) {
        String number = option.clientNumber() == null ? "" : " · N° " + option.clientNumber();
        return option.fullName() + " (" + option.documentNumber() + ")" + number;
    }

    private static String formatMembership(PaymentMembershipOption option) {
        return option.planName() + " · " + CURRENCY.format(option.planPrice()) + " · " + option.statusLabel();
    }

    private static String formatPrimaryDate(PaymentSummary summary) {
        if (summary.paidAt() != null) {
            return formatDateTime(summary.paidAt());
        }
        if (summary.dueAt() != null) {
            return formatDateTime(summary.dueAt());
        }
        return "—";
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : DATE_FORMAT.format(date);
    }

    private static String formatDateTime(OffsetDateTime value) {
        return value == null ? "—" : DATE_FORMAT.format(value.toLocalDate());
    }

    private static String formatMoney(BigDecimal amount) {
        return CURRENCY.format(amount);
    }

    private static String labelForType(PaymentType type) {
        return switch (type) {
            case MEMBERSHIP -> "Membresía";
            case LATE_FEE -> "Mora / recargo";
            case DAILY_PASS -> "Ingreso diario";
        };
    }

    private static String labelForMethod(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case CARD -> "Tarjeta";
            case OTHER -> "Otro";
        };
    }

    private static String labelForStatus(PaymentSummary summary) {
        return labelForStatus(summary.status(), summary.overdue());
    }

    private static String labelForStatus(PaymentStatus status, boolean overdue) {
        if (status == PaymentStatus.PENDING && overdue) {
            return "En mora";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case PAID -> "Cobrado";
            case CANCELLED -> "Cancelado";
        };
    }

    private static String badgeClassForStatus(PaymentStatus status, boolean overdue) {
        if (status == PaymentStatus.PENDING && overdue) {
            return "badge-overdue";
        }
        return switch (status) {
            case PAID -> "badge-paid";
            case PENDING -> "badge-pending";
            case CANCELLED -> "badge-cancelled";
        };
    }

    private void showFeedbackOk(String message) {
        hideFeedback();
        feedbackBanner.getStyleClass().remove("error");
        feedbackLabel.setText(message);
        feedbackBanner.setVisible(true);
        feedbackBanner.setManaged(true);
        feedbackHideDelay.stop();
        feedbackHideDelay.playFromStart();
    }

    private void showFeedbackError(String message) {
        hideFeedback();
        if (!feedbackBanner.getStyleClass().contains("error")) {
            feedbackBanner.getStyleClass().add("error");
        }
        feedbackLabel.setText(message);
        feedbackBanner.setVisible(true);
        feedbackBanner.setManaged(true);
        feedbackHideDelay.stop();
    }

    private void showFormError(String message, javafx.scene.Node field) {
        hideFeedback();
        clearFieldErrors();
        if (field != null) {
            markInvalid(field);
        }
        statusError(message);
    }

    private void statusInfo(String message) {
        statusLabel.getStyleClass().setAll("muted");
        statusLabel.setText(message == null ? "" : message);
    }

    private void statusError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message == null ? "" : message);
    }

    private void bindFieldErrorClearing() {
        clientCombo.valueProperty().addListener((obs, old, value) -> clearInvalid(clientCombo));
        typeCombo.valueProperty().addListener((obs, old, value) -> clearInvalid(typeCombo));
        membershipCombo.valueProperty().addListener((obs, old, value) -> clearInvalid(membershipCombo));
        methodCombo.valueProperty().addListener((obs, old, value) -> clearInvalid(methodCombo));
        amountField.textProperty().addListener((obs, old, value) -> clearInvalid(amountField));
        dueDatePicker.valueProperty().addListener((obs, old, value) -> clearInvalid(dueDatePicker));
    }

    private void clearFieldErrors() {
        clearInvalid(clientCombo);
        clearInvalid(typeCombo);
        clearInvalid(membershipCombo);
        clearInvalid(methodCombo);
        clearInvalid(amountField);
        clearInvalid(dueDatePicker);
    }

    private static void markInvalid(javafx.scene.Node node) {
        if (node != null && !node.getStyleClass().contains("field-invalid")) {
            node.getStyleClass().add("field-invalid");
        }
    }

    private static void clearInvalid(javafx.scene.Node node) {
        if (node != null) {
            node.getStyleClass().remove("field-invalid");
        }
    }

    private javafx.scene.Node fieldForMessage(String message) {
        if (message == null) {
            return null;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("cliente")) {
            return clientCombo;
        }
        if (lower.contains("tipo")) {
            return typeCombo;
        }
        if (lower.contains("membresía") || lower.contains("membresia")) {
            return membershipCombo;
        }
        if (lower.contains("monto")) {
            return amountField;
        }
        if (lower.contains("medio")) {
            return methodCombo;
        }
        return null;
    }

    private void hideFeedback() {
        feedbackHideDelay.stop();
        feedbackBanner.setVisible(false);
        feedbackBanner.setManaged(false);
        feedbackBanner.getStyleClass().remove("error");
        feedbackLabel.setText("");
    }
}
