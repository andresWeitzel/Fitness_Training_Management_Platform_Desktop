package com.fitnesstraining.controller;

import com.fitnesstraining.app.ConfirmDialogs;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.memberships.dto.AssignMembershipRequest;
import com.fitnesstraining.memberships.dto.ClientMembershipOption;
import com.fitnesstraining.memberships.dto.ClientMembershipSummary;
import com.fitnesstraining.memberships.dto.ClientMembershipView;
import com.fitnesstraining.memberships.dto.MembershipPlanRequest;
import com.fitnesstraining.memberships.dto.MembershipPlanSummary;
import com.fitnesstraining.memberships.dto.MembershipPlanView;
import com.fitnesstraining.memberships.model.MembershipBillingMode;
import com.fitnesstraining.memberships.model.MembershipListScope;
import com.fitnesstraining.memberships.model.MembershipStatus;
import com.fitnesstraining.memberships.service.MembershipService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MembershipsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @FXML private Label plansCountLabel;
    @FXML private TextField planSearchField;
    @FXML private Button newPlanButton;
    @FXML private TableView<MembershipPlanSummary> plansTable;
    @FXML private TableColumn<MembershipPlanSummary, String> planNameColumn;
    @FXML private TableColumn<MembershipPlanSummary, String> planDurationColumn;
    @FXML private TableColumn<MembershipPlanSummary, String> planPriceColumn;
    @FXML private TableColumn<MembershipPlanSummary, String> planActiveColumn;

    @FXML private Label planSubtitleLabel;
    @FXML private Label planStatusBadge;
    @FXML private TextField planNameField;
    @FXML private TextArea planDescriptionField;
    @FXML private TextField planDurationField;
    @FXML private TextField planPriceField;
    @FXML private CheckBox planActiveCheck;
    @FXML private Label planStatusLabel;
    @FXML private Button savePlanButton;

    @FXML private HBox feedbackBanner;
    @FXML private Label feedbackLabel;

    @FXML private Label membershipsCountLabel;
    @FXML private Button filterActiveMembershipsButton;
    @FXML private Button filterExpiredMembershipsButton;
    @FXML private Button filterCancelledMembershipsButton;
    @FXML private Button filterAllMembershipsButton;
    @FXML private TextField membershipSearchField;
    @FXML private Button assignMembershipButton;
    @FXML private TableView<ClientMembershipSummary> membershipsTable;
    @FXML private TableColumn<ClientMembershipSummary, String> membershipClientColumn;
    @FXML private TableColumn<ClientMembershipSummary, String> membershipDocumentColumn;
    @FXML private TableColumn<ClientMembershipSummary, String> membershipPlanColumn;
    @FXML private TableColumn<ClientMembershipSummary, String> membershipEndsColumn;
    @FXML private TableColumn<ClientMembershipSummary, String> membershipStatusColumn;

    @FXML private Label membershipSubtitleLabel;
    @FXML private Label membershipStatusBadge;
    @FXML private ComboBox<ClientMembershipOption> clientCombo;
    @FXML private ComboBox<MembershipPlanSummary> planCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<MembershipBillingMode> billingModeCombo;
    @FXML private Label membershipStartsLabel;
    @FXML private Label membershipEndsLabel;
    @FXML private Label membershipDurationLabel;
    @FXML private Label membershipPriceLabel;
    @FXML private Label membershipStatusLabel;
    @FXML private Button saveMembershipButton;
    @FXML private Button renewMembershipButton;
    @FXML private Button cancelMembershipButton;

    private final MembershipService membershipService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    private Long selectedPlanId;
    private Long selectedMembershipId;
    private MembershipStatus selectedMembershipStatus;
    private boolean canManage;
    private MembershipListScope membershipScope = MembershipListScope.ACTIVE;
    private List<MembershipPlanSummary> allPlans = List.of();
    private List<ClientMembershipOption> assignableClients = List.of();
    private final PauseTransition planSearchDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition membershipSearchDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition feedbackHideDelay = new PauseTransition(Duration.seconds(4));

    public MembershipsController(
            MembershipService membershipService,
            SessionContext sessionContext,
            AuthorizationService authorizationService) {
        this.membershipService = membershipService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.MEMBERSHIPS_MANAGE);
        applyPermissions();

        setupPlansTable();
        setupMembershipsTable();
        setupCombos();

        planSearchDelay.setOnFinished(e -> refreshPlans());
        planSearchField.textProperty().addListener((obs, old, value) -> {
            planSearchDelay.stop();
            planSearchDelay.playFromStart();
        });

        membershipSearchDelay.setOnFinished(e -> refreshMemberships());
        membershipSearchField.textProperty().addListener((obs, old, value) -> {
            membershipSearchDelay.stop();
            membershipSearchDelay.playFromStart();
        });

        plansTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadPlan(selected.id());
            }
        });

        membershipsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadMembership(selected.id());
            }
        });

        planCombo.valueProperty().addListener((obs, old, plan) -> updatePlanPreview(plan));
        startDatePicker.valueProperty().addListener((obs, old, value) -> updatePlanPreview(planCombo.getValue()));

        feedbackHideDelay.setOnFinished(e -> hideFeedback());

        onNewPlan();
        onNewMembership();
        Platform.runLater(this::loadInitialData);
    }

    private void loadInitialData() {
        try {
            refreshPlans();
            refreshMemberships();
            refreshAssignableClients();
        } catch (RuntimeException ex) {
            showFeedbackError("Error al cargar membresías: " + ex.getMessage());
        }
    }

    @FXML
    public void onDismissFeedback() {
        hideFeedback();
    }

    @FXML
    public void onNewPlan() {
        selectedPlanId = null;
        planNameField.clear();
        planDescriptionField.clear();
        planDurationField.clear();
        planPriceField.clear();
        planActiveCheck.setSelected(true);
        planSubtitleLabel.setText("Nuevo plan de membresía");
        planStatusBadge.setText("Nuevo");
        planStatusBadge.getStyleClass().removeAll("badge-soon", "badge-ready");
        planStatusBadge.getStyleClass().add("badge-ready");
        planStatusLabel.setText(canManage ? "Complete los datos y guarde el plan." : "Solo lectura.");
        plansTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void onSavePlan() {
        if (!canManage) {
            return;
        }
        try {
            MembershipPlanRequest request = buildPlanRequest();
            boolean creating = selectedPlanId == null;
            MembershipPlanView saved = creating
                    ? membershipService.createPlan(request)
                    : membershipService.updatePlan(selectedPlanId, request);
            selectedPlanId = saved.id();
            showFeedbackOk(creating ? "Plan creado correctamente." : "Plan guardado correctamente.");
            refreshPlans();
            loadPlan(saved.id());
        } catch (ValidationException ex) {
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onFilterActiveMemberships() {
        setMembershipScope(MembershipListScope.ACTIVE);
    }

    @FXML
    public void onFilterExpiredMemberships() {
        setMembershipScope(MembershipListScope.EXPIRED);
    }

    @FXML
    public void onFilterCancelledMemberships() {
        setMembershipScope(MembershipListScope.CANCELLED);
    }

    @FXML
    public void onFilterAllMemberships() {
        setMembershipScope(MembershipListScope.ALL);
    }

    @FXML
    public void onNewMembership() {
        selectedMembershipId = null;
        selectedMembershipStatus = null;
        clientCombo.setDisable(!canManage);
        planCombo.setDisable(!canManage);
        startDatePicker.setDisable(!canManage);
        billingModeCombo.setDisable(!canManage);
        startDatePicker.setValue(null);
        resetBillingMode();
        clientCombo.getSelectionModel().clearSelection();
        planCombo.getSelectionModel().clearSelection();
        membershipSubtitleLabel.setText("Asignar membresía a un cliente");
        membershipStatusBadge.setText("Nueva");
        membershipStartsLabel.setText("—");
        membershipEndsLabel.setText("—");
        membershipDurationLabel.setText("—");
        membershipPriceLabel.setText("—");
        membershipStatusLabel.setText(canManage
                ? "Seleccione cliente, plan y cobro, luego asigne."
                : "Solo lectura.");
        saveMembershipButton.setText("Asignar membresía");
        saveMembershipButton.setDisable(!canManage);
        renewMembershipButton.setDisable(true);
        cancelMembershipButton.setDisable(true);
        membershipsTable.getSelectionModel().clearSelection();
        applyMembershipBadge(null);
    }

    @FXML
    public void onSaveMembership() {
        if (!canManage) {
            return;
        }
        if (selectedMembershipId == null) {
            assignNewMembership();
            return;
        }
        if (selectedMembershipStatus == MembershipStatus.ACTIVE) {
            changeSelectedPlan();
            return;
        }
        reassignSelectedMembership();
    }

    private void assignNewMembership() {
        try {
            ClientMembershipOption client = clientCombo.getValue();
            MembershipPlanSummary plan = planCombo.getValue();
            if (client == null || plan == null) {
                throw new ValidationException("Seleccione cliente y plan.");
            }
            AssignMembershipRequest request = new AssignMembershipRequest(
                    client.clientId(),
                    plan.id(),
                    startDatePicker.getValue(),
                    selectedBillingMode());
            ClientMembershipView saved = membershipService.assignMembership(request);
            showFeedbackOk("Membresía asignada correctamente.");
            setMembershipScope(MembershipListScope.ACTIVE);
            refreshMemberships();
            selectMembershipInTable(saved.id());
            loadMembership(saved.id());
        } catch (ValidationException ex) {
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    private void changeSelectedPlan() {
        try {
            MembershipPlanSummary plan = planCombo.getValue();
            if (plan == null) {
                throw new ValidationException("Seleccione el nuevo plan.");
            }
            ClientMembershipView saved = membershipService.changePlan(
                    selectedMembershipId, plan.id(), selectedBillingMode());
            showFeedbackOk("Plan actualizado: " + saved.planName() + ".");
            refreshMemberships();
            selectMembershipInTable(saved.id());
            loadMembership(saved.id());
        } catch (ValidationException ex) {
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    private void reassignSelectedMembership() {
        try {
            MembershipPlanSummary plan = planCombo.getValue();
            if (plan == null) {
                throw new ValidationException("Seleccione un plan para reasignar.");
            }
            ClientMembershipView saved = membershipService.reassignMembership(
                    selectedMembershipId,
                    plan.id(),
                    startDatePicker.getValue(),
                    selectedBillingMode());
            showFeedbackOk("Membresía reasignada y activa.");
            setMembershipScope(MembershipListScope.ACTIVE);
            refreshMemberships();
            selectMembershipInTable(saved.id());
            loadMembership(saved.id());
        } catch (ValidationException ex) {
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onRenewMembership() {
        if (!canManage || selectedMembershipId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(
                renewMembershipButton,
                "Renovar membresía",
                "¿Renovar esta membresía?",
                "Se extenderá la vigencia según la duración del plan actual.")) {
            return;
        }
        try {
            ClientMembershipView renewed = membershipService.renewMembership(
                    selectedMembershipId, selectedBillingMode());
            showFeedbackOk("Membresía renovada.");
            refreshMemberships();
            selectMembershipInTable(renewed.id());
            loadMembership(renewed.id());
        } catch (ValidationException ex) {
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    @FXML
    public void onCancelMembership() {
        if (!canManage || selectedMembershipId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(
                cancelMembershipButton,
                "Cancelar membresía",
                "¿Cancelar esta membresía?",
                "Después podrá reasignar otro plan al mismo cliente.")) {
            return;
        }
        try {
            ClientMembershipView cancelled = membershipService.cancelMembership(selectedMembershipId);
            showFeedbackOk("Membresía cancelada. Elija un plan y pulse Reasignar.");
            if (membershipScope == MembershipListScope.ACTIVE) {
                setMembershipScope(MembershipListScope.CANCELLED);
            } else {
                refreshMemberships();
            }
            selectMembershipInTable(cancelled.id());
            loadMembership(cancelled.id());
        } catch (ValidationException ex) {
            showFeedbackError(ex.getMessage());
        } catch (RuntimeException ex) {
            showFeedbackError(ex.getMessage());
        }
    }

    private void setupPlansTable() {
        planNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        planDurationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().durationDays() + " días"));
        planPriceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatMoney(data.getValue().price())));
        planActiveColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(labelForStatus(item, true));
                badge.getStyleClass().add(badgeClassForStatus(item, true));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        planActiveColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().active() ? "ACTIVE" : "INACTIVE"));
        plansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupMembershipsTable() {
        membershipClientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientName()));
        membershipDocumentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientDocument()));
        membershipPlanColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().planName()));
        membershipEndsColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue().endsAt().toLocalDate())));
        membershipStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(labelForStatus(item, false));
                badge.getStyleClass().add(badgeClassForStatus(item, false));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        membershipStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().status().name()));
        membershipsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupCombos() {
        clientCombo.setConverter(clientConverter());
        clientCombo.setCellFactory(listView -> clientOptionCell());
        clientCombo.setButtonCell(clientButtonCell());

        planCombo.setConverter(planConverter());
        planCombo.setCellFactory(listView -> planOptionCell());
        planCombo.setButtonCell(planButtonCell());

        billingModeCombo.setItems(FXCollections.observableArrayList(
                MembershipBillingMode.PENDING,
                MembershipBillingMode.PAID,
                MembershipBillingMode.COMPLIMENTARY));
        billingModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MembershipBillingMode mode) {
                return labelForBilling(mode);
            }

            @Override
            public MembershipBillingMode fromString(String string) {
                return null;
            }
        });
        resetBillingMode();
    }

    private MembershipBillingMode selectedBillingMode() {
        MembershipBillingMode mode = billingModeCombo.getValue();
        return mode == null ? MembershipBillingMode.PENDING : mode;
    }

    private void resetBillingMode() {
        billingModeCombo.setValue(MembershipBillingMode.PENDING);
    }

    private static String labelForBilling(MembershipBillingMode mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case PENDING -> "Pendiente (genera cobro)";
            case PAID -> "Cobrado ahora";
            case COMPLIMENTARY -> "Cortesía (sin cobro)";
        };
    }

    private StringConverter<ClientMembershipOption> clientConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(ClientMembershipOption option) {
                return option == null ? "" : option.fullName();
            }

            @Override
            public ClientMembershipOption fromString(String string) {
                return null;
            }
        };
    }

    private StringConverter<MembershipPlanSummary> planConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(MembershipPlanSummary plan) {
                if (plan == null) {
                    return "";
                }
                return plan.name() + " · " + plan.durationDays() + " días · " + formatMoney(plan.price());
            }

            @Override
            public MembershipPlanSummary fromString(String string) {
                return null;
            }
        };
    }

    private ListCell<ClientMembershipOption> clientOptionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ClientMembershipOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label name = new Label(item.fullName());
                name.getStyleClass().add("combo-primary");
                String number = item.clientNumber() == null || item.clientNumber().isBlank()
                        ? "Sin n° cliente"
                        : item.clientNumber();
                Label meta = new Label(item.documentNumber() + " · " + number);
                meta.getStyleClass().add("combo-secondary");
                VBox box = new VBox(2, name, meta);
                setGraphic(box);
                setText(null);
            }
        };
    }

    private ListCell<ClientMembershipOption> clientButtonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ClientMembershipOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.fullName() + " · " + item.documentNumber());
            }
        };
    }

    private ListCell<MembershipPlanSummary> planOptionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(MembershipPlanSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label name = new Label(item.name());
                name.getStyleClass().add("combo-primary");
                Label meta = new Label(item.durationDays() + " días · " + formatMoney(item.price()));
                meta.getStyleClass().add("combo-secondary");
                setGraphic(new VBox(2, name, meta));
                setText(null);
            }
        };
    }

    private ListCell<MembershipPlanSummary> planButtonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(MembershipPlanSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        };
    }

    private void refreshPlans() {
        allPlans = membershipService.listPlans();
        String term = planSearchField.getText();
        List<MembershipPlanSummary> filtered = term == null || term.isBlank()
                ? allPlans
                : allPlans.stream()
                .filter(plan -> plan.name().toLowerCase().contains(term.trim().toLowerCase()))
                .toList();
        plansTable.setItems(FXCollections.observableArrayList(filtered));
        plansCountLabel.setText(filtered.size() + " plan(es)");
        planCombo.setItems(FXCollections.observableArrayList(
                allPlans.stream().filter(MembershipPlanSummary::active).toList()));
    }

    private void refreshMemberships() {
        String term = membershipSearchField.getText();
        List<ClientMembershipSummary> items = membershipService.listMemberships(term, membershipScope);
        membershipsTable.setItems(FXCollections.observableArrayList(items));
        membershipsCountLabel.setText(items.size() + " registro(s)");
    }

    private void refreshAssignableClients() {
        assignableClients = membershipService.listAssignableClients();
        clientCombo.setItems(FXCollections.observableArrayList(assignableClients));
    }

    private void loadPlan(Long id) {
        MembershipPlanView view = membershipService.getPlan(id);
        selectedPlanId = view.id();
        planNameField.setText(view.name());
        planDescriptionField.setText(view.description() == null ? "" : view.description());
        planDurationField.setText(String.valueOf(view.durationDays()));
        planPriceField.setText(view.price().toPlainString());
        planActiveCheck.setSelected(view.active());
        planSubtitleLabel.setText(view.name());
        planStatusBadge.setText(view.active() ? "Activo" : "Inactivo");
        planStatusBadge.getStyleClass().removeAll("badge-soon", "badge-ready");
        planStatusBadge.getStyleClass().add(view.active() ? "badge-ready" : "badge-soon");
        planStatusLabel.setText("Plan #" + view.id());
    }

    private void loadMembership(Long id) {
        ClientMembershipView view = membershipService.getMembership(id);
        selectedMembershipId = view.id();
        selectedMembershipStatus = view.status();

        selectClient(view.clientId());
        selectPlan(view.planId());
        startDatePicker.setValue(null);
        resetBillingMode();

        membershipSubtitleLabel.setText(view.clientName() + " · " + view.planName());
        membershipStartsLabel.setText(formatDate(view.startsAt().toLocalDate()));
        membershipEndsLabel.setText(formatDate(view.endsAt().toLocalDate()));
        membershipDurationLabel.setText(view.durationDays() + " días");
        membershipPriceLabel.setText(formatMoney(view.planPrice()));
        applyMembershipBadge(view.status());

        clientCombo.setDisable(true);
        boolean editable = canManage;
        planCombo.setDisable(!editable);
        billingModeCombo.setDisable(!editable);

        if (view.status() == MembershipStatus.ACTIVE) {
            startDatePicker.setDisable(true);
            saveMembershipButton.setText("Cambiar plan");
            saveMembershipButton.setDisable(!editable);
            renewMembershipButton.setDisable(!editable);
            cancelMembershipButton.setDisable(!editable);
            membershipStatusLabel.setText(editable
                    ? "Puede cambiar el plan, renovar o cancelar. Elija el cobro para la operación."
                    : "Solo lectura.");
            return;
        }

        startDatePicker.setDisable(!editable);
        saveMembershipButton.setText("Reasignar membresía");
        saveMembershipButton.setDisable(!editable);
        renewMembershipButton.setDisable(true);
        cancelMembershipButton.setDisable(true);
        membershipStatusLabel.setText(editable
                ? "Membresía " + labelForStatus(view.status().name(), false).toLowerCase()
                + ". Elija un plan y pulse Reasignar para volver a activarla."
                : "Solo lectura.");
    }

    private void selectMembershipInTable(Long id) {
        membershipsTable.getItems().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .ifPresent(item -> membershipsTable.getSelectionModel().select(item));
    }

    private void selectClient(Long clientId) {
        assignableClients.stream()
                .filter(option -> option.clientId().equals(clientId))
                .findFirst()
                .ifPresentOrElse(clientCombo::setValue, () -> {
                    ClientMembershipView view = membershipService.getMembership(selectedMembershipId);
                    ClientMembershipOption synthetic = new ClientMembershipOption(
                            view.clientId(),
                            view.clientDocument(),
                            view.clientName(),
                            null);
                    clientCombo.getItems().add(synthetic);
                    clientCombo.setValue(synthetic);
                });
    }

    private void selectPlan(Long planId) {
        allPlans.stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst()
                .ifPresent(planCombo::setValue);
    }

    private void updatePlanPreview(MembershipPlanSummary plan) {
        if (plan == null) {
            return;
        }
        if (selectedMembershipId != null && selectedMembershipStatus == MembershipStatus.ACTIVE) {
            membershipDurationLabel.setText(plan.durationDays() + " días");
            membershipPriceLabel.setText(formatMoney(plan.price()));
            LocalDate start = LocalDate.now();
            membershipStartsLabel.setText(formatDate(start));
            membershipEndsLabel.setText(formatDate(start.plusDays(plan.durationDays())));
            return;
        }
        if (selectedMembershipId != null && selectedMembershipStatus != MembershipStatus.ACTIVE) {
            membershipDurationLabel.setText(plan.durationDays() + " días");
            membershipPriceLabel.setText(formatMoney(plan.price()));
            LocalDate start = startDatePicker.getValue() == null ? LocalDate.now() : startDatePicker.getValue();
            membershipStartsLabel.setText(formatDate(start));
            membershipEndsLabel.setText(formatDate(start.plusDays(plan.durationDays())));
            return;
        }
        membershipDurationLabel.setText(plan.durationDays() + " días");
        membershipPriceLabel.setText(formatMoney(plan.price()));
        LocalDate start = startDatePicker.getValue() == null ? LocalDate.now() : startDatePicker.getValue();
        membershipStartsLabel.setText(formatDate(start));
        membershipEndsLabel.setText(formatDate(start.plusDays(plan.durationDays())));
    }

    private MembershipPlanRequest buildPlanRequest() {
        BigDecimal price;
        try {
            String raw = planPriceField.getText() == null ? "0" : planPriceField.getText().trim().replace(',', '.');
            price = new BigDecimal(raw.isEmpty() ? "0" : raw);
        } catch (NumberFormatException ex) {
            throw new ValidationException("El precio no es válido.");
        }
        int duration;
        try {
            duration = Integer.parseInt(planDurationField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("La duración debe ser un número entero.");
        }
        return new MembershipPlanRequest(
                planNameField.getText(),
                planDescriptionField.getText(),
                duration,
                price,
                planActiveCheck.isSelected());
    }

    private void setMembershipScope(MembershipListScope scope) {
        membershipScope = scope;
        applyChipState(filterActiveMembershipsButton, scope == MembershipListScope.ACTIVE);
        applyChipState(filterExpiredMembershipsButton, scope == MembershipListScope.EXPIRED);
        applyChipState(filterCancelledMembershipsButton, scope == MembershipListScope.CANCELLED);
        applyChipState(filterAllMembershipsButton, scope == MembershipListScope.ALL);
        refreshMemberships();
    }

    private static void applyChipState(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        button.getStyleClass().remove("selected");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private void applyMembershipBadge(MembershipStatus status) {
        membershipStatusBadge.getStyleClass().removeAll("badge-soon", "badge-ready");
        if (status == null) {
            membershipStatusBadge.setText("Nueva");
            membershipStatusBadge.getStyleClass().add("badge-ready");
            return;
        }
        membershipStatusBadge.setText(labelForStatus(status.name(), false));
        membershipStatusBadge.getStyleClass().add(badgeClassForStatus(status.name(), false));
    }

    private void applyPermissions() {
        newPlanButton.setDisable(!canManage);
        savePlanButton.setDisable(!canManage);
        planNameField.setEditable(canManage);
        planDescriptionField.setEditable(canManage);
        planDurationField.setEditable(canManage);
        planPriceField.setEditable(canManage);
        planActiveCheck.setDisable(!canManage);

        assignMembershipButton.setDisable(!canManage);
        saveMembershipButton.setDisable(!canManage);
        clientCombo.setDisable(!canManage);
        planCombo.setDisable(!canManage);
        startDatePicker.setDisable(!canManage);
        billingModeCombo.setDisable(!canManage);
        renewMembershipButton.setDisable(!canManage);
        cancelMembershipButton.setDisable(!canManage);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : DATE_FORMAT.format(date);
    }

    private static String formatMoney(BigDecimal amount) {
        return CURRENCY.format(amount);
    }

    private static String labelForStatus(String status, boolean planMode) {
        if (planMode) {
            return "ACTIVE".equals(status) ? "Activo" : "Inactivo";
        }
        return switch (status) {
            case "ACTIVE" -> "Activa";
            case "EXPIRED" -> "Vencida";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    private static String badgeClassForStatus(String status, boolean planMode) {
        if (planMode) {
            return "ACTIVE".equals(status) ? "badge-ready" : "badge-soon";
        }
        return switch (status) {
            case "ACTIVE" -> "badge-ready";
            case "EXPIRED", "CANCELLED" -> "badge-soon";
            default -> "badge-soon";
        };
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
}
