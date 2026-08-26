package com.fitnesstraining.controller;

import com.fitnesstraining.app.ConfirmDialogs;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.ClientListScope;
import com.fitnesstraining.app.TableStatusCells;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.service.ClientService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.util.Duration;

public class ClientsController {

    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private Button filterActiveButton;
    @FXML private Button filterInactiveButton;
    @FXML private Button filterAllButton;
    @FXML private TableView<ClientSummary> clientsTable;
    @FXML private TableColumn<ClientSummary, String> documentColumn;
    @FXML private TableColumn<ClientSummary, String> nameColumn;
    @FXML private TableColumn<ClientSummary, String> numberColumn;
    @FXML private TableColumn<ClientSummary, String> statusColumn;
    @FXML private TableColumn<ClientSummary, String> detailColumn;

    @FXML private Label fichaSubtitleLabel;
    @FXML private Label fichaStatusBadge;
    @FXML private TextField documentField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Label statusLabel;

    @FXML private Button newButton;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;

    private final ClientService clientService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    private Long selectedId;
    private boolean canManage;
    private boolean selectedInactive;
    private ClientListScope listScope = ClientListScope.ACTIVE;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(180));

    public ClientsController(
            ClientService clientService,
            SessionContext sessionContext,
            AuthorizationService authorizationService) {
        this.clientService = clientService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.CLIENTS_MANAGE);
        applyPermissions();

        documentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().documentNumber()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
        numberColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().clientNumber() == null || data.getValue().clientNumber().isBlank()
                        ? "—"
                        : data.getValue().clientNumber()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(
                ClientStatus.ACTIVE.name().equals(data.getValue().status().name()) ? "Activo" : "Baja"));
        statusColumn.setCellFactory(col -> TableStatusCells.of((row, item) ->
                row.status() == ClientStatus.ACTIVE ? "badge-paid" : "badge-cancelled"));
        detailColumn.setCellFactory(col -> detailActionCell());
        detailColumn.setSortable(false);
        clientsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        clientsTable.setPlaceholder(new Label("No hay clientes para este filtro."));

        clientsTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            if (selected != null) {
                loadClient(selected.id());
            }
        });
        searchDelay.setOnFinished(event -> reloadTable());
        searchField.textProperty().addListener((obs, old, value) -> searchDelay.playFromStart());
        searchField.setOnAction(event -> {
            searchDelay.stop();
            reloadTable();
        });
        reloadTable();
        onNew();
    }

    private TableCell<ClientSummary, String> detailActionCell() {
        return new TableCell<>() {
            private final Button detail = new Button("ⓘ");

            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver detalle operativo y credenciales"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < clientsTable.getItems().size()) {
                        openDetail(clientsTable.getItems().get(getIndex()).id());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= clientsTable.getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private void openDetail(Long clientId) {
        try {
            ClientView view = clientService.get(clientId);
            Window owner = clientsTable.getScene() == null ? null : clientsTable.getScene().getWindow();
            ClientDetailController.open(owner, view, clientService, canManage, updated -> {
                reloadTable();
                if (selectedId != null && selectedId.equals(updated.id())) {
                    show(updated);
                }
            });
        } catch (RuntimeException ex) {
            statusError(ex.getMessage());
        }
    }

    @FXML
    public void onFilterActive() {
        setScope(ClientListScope.ACTIVE);
    }

    @FXML
    public void onFilterInactive() {
        setScope(ClientListScope.INACTIVE);
    }

    @FXML
    public void onFilterAll() {
        setScope(ClientListScope.ALL);
    }

    @FXML
    public void onSearch() {
        reloadTable();
    }

    @FXML
    public void onNew() {
        selectedId = null;
        selectedInactive = false;
        clientsTable.getSelectionModel().clearSelection();
        documentField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressField.setText("");
        fichaSubtitleLabel.setText("Alta nueva. Al guardar se genera el n° de cliente.");
        fichaStatusBadge.setText("Nuevo");
        fichaStatusBadge.getStyleClass().setAll("badge-soon");
        statusLabel.setText("");
        statusLabel.getStyleClass().setAll("muted");
        applyFormState(true);
        deactivateButton.setDisable(true);
    }

    @FXML
    public void onSave() {
        try {
            ClientRequest request = readForm();
            ClientView saved = selectedId == null
                    ? clientService.create(request)
                    : clientService.update(selectedId, request);
            statusOk(selectedId == null
                    ? "Cliente registrado con membresía Mensual asignada."
                    : "Cambios guardados.");
            reloadTable();
            selectById(saved.id());
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
        } catch (Exception ex) {
            statusError("No se pudo guardar: " + ex.getMessage());
        }
    }

    @FXML
    public void onDeactivate() {
        if (selectedId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(
                deactivateButton,
                "Dar de baja",
                "¿Dar de baja al cliente?",
                "No se borra el historial. El documento queda disponible para un alta nueva.")) {
            return;
        }
        try {
            clientService.deactivate(selectedId);
            statusOk("Cliente dado de baja.");
            if (listScope == ClientListScope.ACTIVE) {
                setScope(ClientListScope.INACTIVE);
            } else {
                reloadTable();
            }
            selectById(selectedId);
        } catch (Exception ex) {
            statusError(ex.getMessage());
        }
    }

    private void setScope(ClientListScope scope) {
        listScope = scope;
        applyChipState(filterActiveButton, scope == ClientListScope.ACTIVE);
        applyChipState(filterInactiveButton, scope == ClientListScope.INACTIVE);
        applyChipState(filterAllButton, scope == ClientListScope.ALL);
        reloadTable();
    }

    private static void applyChipState(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        button.getStyleClass().remove("selected");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private void loadClient(Long id) {
        try {
            show(clientService.get(id));
        } catch (Exception ex) {
            statusError(ex.getMessage());
        }
    }

    private void show(ClientView view) {
        selectedId = view.id();
        selectedInactive = view.status() != ClientStatus.ACTIVE;
        documentField.setText(view.documentNumber());
        firstNameField.setText(view.firstName());
        lastNameField.setText(view.lastName());
        emailField.setText(nullToEmpty(view.email()));
        phoneField.setText(nullToEmpty(view.phone()));
        addressField.setText(nullToEmpty(view.address()));

        String number = view.credentials().stream()
                .filter(item -> item.type() == CredentialType.CLIENT_NUMBER)
                .map(CredentialView::code)
                .findFirst()
                .orElse("sin n°");
        fichaSubtitleLabel.setText(view.documentNumber() + " · " + number);
        fichaStatusBadge.setText(selectedInactive ? "Baja" : "Activo");
        fichaStatusBadge.getStyleClass().setAll(selectedInactive ? "badge-soon" : "badge-ready");

        applyFormState(!selectedInactive);
        deactivateButton.setDisable(!canManage || selectedInactive);
        if (selectedInactive) {
            statusLabel.getStyleClass().setAll("muted");
            statusLabel.setText("Cliente dado de baja. Solo consulta; el DNI queda libre para un alta nueva. Usá ⓘ para ver resumen y credenciales.");
        } else if (statusLabel.getText() == null || statusLabel.getText().isBlank()
                || statusLabel.getStyleClass().contains("muted")) {
            statusLabel.getStyleClass().setAll("muted");
            statusLabel.setText("Usá ⓘ en el listado para ver resumen operativo y credenciales.");
        }
    }

    private void applyFormState(boolean editable) {
        boolean write = canManage && editable;
        documentField.setDisable(selectedId != null);
        firstNameField.setEditable(write);
        lastNameField.setEditable(write);
        emailField.setEditable(write);
        phoneField.setEditable(write);
        addressField.setEditable(write);
        saveButton.setDisable(!write);
        newButton.setDisable(!canManage);
    }

    private void reloadTable() {
        var items = FXCollections.observableArrayList(clientService.list(searchField.getText(), listScope));
        clientsTable.setItems(items);
        int count = items.size();
        String noun = count == 1 ? "cliente" : "clientes";
        resultCountLabel.setText(count + " " + noun);
    }

    private void selectById(Long id) {
        if (id == null) {
            return;
        }
        clientsTable.getItems().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .ifPresent(item -> clientsTable.getSelectionModel().select(item));
    }

    private ClientRequest readForm() {
        return new ClientRequest(
                documentField.getText(),
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                addressField.getText()
        );
    }

    private void applyPermissions() {
        saveButton.setDisable(!canManage);
        newButton.setDisable(!canManage);
        if (!canManage) {
            statusLabel.setText("Solo lectura: su rol puede consultar clientes.");
        }
    }

    private void statusOk(String message) {
        statusLabel.getStyleClass().setAll("status-ok");
        statusLabel.setText(message);
    }

    private void statusError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
