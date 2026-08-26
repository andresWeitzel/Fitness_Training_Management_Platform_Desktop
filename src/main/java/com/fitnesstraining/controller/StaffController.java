package com.fitnesstraining.controller;

import com.fitnesstraining.app.ConfirmDialogs;
import com.fitnesstraining.app.TableStatusCells;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.staff.dto.StaffRequest;
import com.fitnesstraining.staff.dto.StaffRoleOption;
import com.fitnesstraining.staff.dto.StaffSummary;
import com.fitnesstraining.staff.dto.StaffView;
import com.fitnesstraining.staff.model.StaffListScope;
import com.fitnesstraining.staff.service.StaffService;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StaffController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label resultCountLabel;
    @FXML private Button filterActiveButton;
    @FXML private Button filterInactiveButton;
    @FXML private Button filterAllButton;
    @FXML private TextField searchField;
    @FXML private Button newButton;
    @FXML private TableView<StaffSummary> staffTable;
    @FXML private TableColumn<StaffSummary, String> usernameColumn;
    @FXML private TableColumn<StaffSummary, String> nameColumn;
    @FXML private TableColumn<StaffSummary, String> roleColumn;
    @FXML private TableColumn<StaffSummary, String> statusColumn;

    @FXML private Label fichaSubtitleLabel;
    @FXML private Label fichaStatusBadge;
    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<StaffRoleOption> roleCombo;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordHintLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;
    @FXML private Button reactivateButton;

    private final StaffService staffService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    private Long selectedId;
    private boolean selectedInactive;
    private boolean canManage;
    private StaffListScope scope = StaffListScope.ACTIVE;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(180));

    public StaffController(
            StaffService staffService,
            SessionContext sessionContext,
            AuthorizationService authorizationService) {
        this.staffService = staffService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.STAFF_MANAGE);
        applyPermissions();
        setupTable();
        setupRoleCombo();

        searchDelay.setOnFinished(e -> reloadTable());
        searchField.textProperty().addListener((obs, old, value) -> {
            searchDelay.stop();
            searchDelay.playFromStart();
        });
        staffTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadStaff(selected.id());
            }
        });

        onNew();
        Platform.runLater(this::reloadTable);
    }

    @FXML
    public void onFilterActive() {
        setScope(StaffListScope.ACTIVE);
    }

    @FXML
    public void onFilterInactive() {
        setScope(StaffListScope.INACTIVE);
    }

    @FXML
    public void onFilterAll() {
        setScope(StaffListScope.ALL);
    }

    @FXML
    public void onNew() {
        selectedId = null;
        selectedInactive = false;
        staffTable.getSelectionModel().clearSelection();
        usernameField.clear();
        displayNameField.clear();
        emailField.clear();
        passwordField.clear();
        if (!roleCombo.getItems().isEmpty()) {
            roleCombo.setValue(roleCombo.getItems().get(0));
        }
        fichaSubtitleLabel.setText("Nuevo usuario interno");
        fichaStatusBadge.setText("Nuevo");
        fichaStatusBadge.getStyleClass().setAll("badge-ready");
        lastLoginLabel.setText("—");
        createdAtLabel.setText("—");
        statusInfo(canManage
                ? "Complete usuario, nombre, rol y contraseña. Luego Guardar."
                : "Solo lectura.");
        applyFormMode();
    }

    @FXML
    public void onSave() {
        if (!canManage) {
            return;
        }
        try {
            StaffRequest request = buildRequest();
            StaffView saved = selectedId == null
                    ? staffService.create(request)
                    : staffService.update(selectedId, request);
            statusOk(selectedId == null ? "Usuario creado." : "Cambios guardados.");
            selectedId = saved.id();
            reloadTable();
            selectById(saved.id());
            loadStaff(saved.id());
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
        } catch (RuntimeException ex) {
            statusError("No se pudo guardar: " + ex.getMessage());
        }
    }

    @FXML
    public void onDeactivate() {
        if (!canManage || selectedId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(
                deactivateButton,
                "Dar de baja",
                "¿Dar de baja a este usuario?",
                "No podrá iniciar sesión. El usuario queda inactivo.")) {
            return;
        }
        try {
            Long actingId = sessionContext.requireUser().id();
            StaffView view = staffService.deactivate(selectedId, actingId);
            statusOk("Usuario dado de baja.");
            if (scope == StaffListScope.ACTIVE) {
                setScope(StaffListScope.INACTIVE);
            } else {
                reloadTable();
            }
            selectById(view.id());
            loadStaff(view.id());
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
        } catch (RuntimeException ex) {
            statusError(ex.getMessage());
        }
    }

    @FXML
    public void onReactivate() {
        if (!canManage || selectedId == null) {
            return;
        }
        try {
            StaffView view = staffService.reactivate(selectedId);
            statusOk("Usuario reactivado.");
            if (scope == StaffListScope.INACTIVE) {
                setScope(StaffListScope.ACTIVE);
            } else {
                reloadTable();
            }
            selectById(view.id());
            loadStaff(view.id());
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
        } catch (RuntimeException ex) {
            statusError(ex.getMessage());
        }
    }

    private void setupTable() {
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayName()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().roleLabel()));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().active() ? "Activo" : "Baja"));
        statusColumn.setCellFactory(col -> TableStatusCells.of((row, item) ->
                row.active() ? "badge-paid" : "badge-cancelled"));
    }

    private void setupRoleCombo() {
        List<StaffRoleOption> roles = staffService.listRoles();
        roleCombo.setItems(FXCollections.observableArrayList(roles));
        roleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(StaffRoleOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public StaffRoleOption fromString(String string) {
                return null;
            }
        });
        roleCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(StaffRoleOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.label() + " — " + item.description());
            }
        });
    }

    private void reloadTable() {
        List<StaffSummary> rows = staffService.list(searchField.getText(), scope);
        staffTable.setItems(FXCollections.observableArrayList(rows));
        resultCountLabel.setText(rows.size() + (rows.size() == 1 ? " usuario" : " usuarios"));
    }

    private void loadStaff(Long id) {
        try {
            StaffView view = staffService.get(id);
            selectedId = view.id();
            selectedInactive = !view.active();
            usernameField.setText(view.username());
            displayNameField.setText(view.displayName());
            emailField.setText(view.email() == null ? "" : view.email());
            passwordField.clear();
            roleCombo.getItems().stream()
                    .filter(option -> option.role() == view.role())
                    .findFirst()
                    .ifPresent(roleCombo::setValue);
            fichaSubtitleLabel.setText("Editar · @" + view.username());
            fichaStatusBadge.setText(view.active() ? "Activo" : "Baja");
            fichaStatusBadge.getStyleClass().setAll(view.active() ? "badge-paid" : "badge-cancelled");
            lastLoginLabel.setText(formatDateTime(view.lastLoginAt()));
            createdAtLabel.setText(formatDateTime(view.createdAt()));
            statusInfo(view.active()
                    ? "Puede cambiar usuario, nombre, email, rol y contraseña desde esta ficha."
                    : "Usuario inactivo. Reactívelo para poder editarlo.");
            applyFormMode();
        } catch (RuntimeException ex) {
            statusError(ex.getMessage());
        }
    }

    private void selectById(Long id) {
        staffTable.getItems().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .ifPresent(row -> staffTable.getSelectionModel().select(row));
    }

    private StaffRequest buildRequest() {
        StaffRoleOption role = roleCombo.getValue();
        return new StaffRequest(
                usernameField.getText(),
                displayNameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                role == null ? null : role.role());
    }

    private void setScope(StaffListScope next) {
        scope = next;
        applyChipState(filterActiveButton, next == StaffListScope.ACTIVE);
        applyChipState(filterInactiveButton, next == StaffListScope.INACTIVE);
        applyChipState(filterAllButton, next == StaffListScope.ALL);
        reloadTable();
    }

    private void applyFormMode() {
        boolean creating = selectedId == null;
        boolean editable = canManage && (creating || !selectedInactive);
        usernameField.setEditable(editable);
        usernameField.setDisable(!editable);
        displayNameField.setEditable(editable);
        displayNameField.setDisable(!editable);
        emailField.setEditable(editable);
        emailField.setDisable(!editable);
        roleCombo.setDisable(!editable);
        passwordField.setEditable(editable);
        passwordField.setDisable(!editable);
        passwordHintLabel.setText(creating
                ? "La contraseña es obligatoria al crear el usuario."
                : "Deje la contraseña vacía para conservarla; complete una nueva para cambiarla.");
        saveButton.setDisable(!editable);
        deactivateButton.setDisable(!canManage || creating || selectedInactive);
        reactivateButton.setDisable(!canManage || creating || !selectedInactive);
        reactivateButton.setVisible(!creating && selectedInactive);
        reactivateButton.setManaged(!creating && selectedInactive);
    }

    private void applyPermissions() {
        newButton.setDisable(!canManage);
        saveButton.setDisable(!canManage);
        deactivateButton.setDisable(!canManage);
        reactivateButton.setDisable(!canManage);
        if (!canManage) {
            statusInfo("Solo lectura: su rol no administra personal.");
        }
    }

    private static void applyChipState(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private void statusInfo(String message) {
        statusLabel.getStyleClass().setAll("muted");
        statusLabel.setText(message == null ? "" : message);
    }

    private void statusOk(String message) {
        statusLabel.getStyleClass().setAll("status-ok");
        statusLabel.setText(message);
    }

    private void statusError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message);
    }

    private static String formatDateTime(OffsetDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }
}
