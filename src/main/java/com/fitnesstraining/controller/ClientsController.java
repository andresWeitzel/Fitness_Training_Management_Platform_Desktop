package com.fitnesstraining.controller;

import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.ClientListScope;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.service.ClientService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ClientsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

    @FXML private Label fichaSubtitleLabel;
    @FXML private Label fichaStatusBadge;
    @FXML private TextField documentField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private VBox credentialsBox;
    @FXML private Label statusLabel;

    @FXML private Button newButton;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;
    @FXML private Button issueCardButton;
    @FXML private Button renewCardButton;
    @FXML private Button issueQrButton;

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
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean active = ClientStatus.ACTIVE.name().equals(item);
                Label badge = new Label(active ? "Activo" : "Baja");
                badge.getStyleClass().add(active ? "badge-ready" : "badge-soon");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
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
        credentialsBox.getChildren().setAll(hint("Después de guardar se puede emitir carnet y QR."));
        statusLabel.setText("");
        statusLabel.getStyleClass().setAll("muted");
        applyFormState(true);
        deactivateButton.setDisable(true);
        issueCardButton.setDisable(true);
        renewCardButton.setDisable(true);
        issueQrButton.setDisable(true);
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Dar de baja");
        confirm.setHeaderText("Dar de baja al cliente");
        confirm.setContentText("No se borra el historial. El documento queda disponible para un alta nueva.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
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

    @FXML
    public void onIssueCard() {
        runCredential(() -> clientService.issueCard(selectedId), "Carnet emitido.");
    }

    @FXML
    public void onRenewCard() {
        runCredential(() -> clientService.renewCard(selectedId), "Carnet renovado por 12 meses.");
    }

    @FXML
    public void onIssueQr() {
        runCredential(() -> clientService.issueQr(selectedId), "Código QR generado.");
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

    private void runCredential(java.util.function.Supplier<ClientView> action, String success) {
        if (selectedId == null) {
            statusError("Seleccione o guarde un cliente primero.");
            return;
        }
        try {
            ClientView view = action.get();
            statusOk(success);
            show(view);
            reloadTable();
            selectById(view.id());
        } catch (ValidationException ex) {
            statusError(ex.getMessage());
        } catch (Exception ex) {
            statusError(ex.getMessage());
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

        renderCredentials(view);
        applyFormState(!selectedInactive);
        deactivateButton.setDisable(!canManage || selectedInactive);
        updateCredentialButtons(view);
        if (selectedInactive) {
            statusLabel.getStyleClass().setAll("muted");
            statusLabel.setText("Cliente dado de baja. Solo consulta; el DNI queda libre para un alta nueva.");
        } else {
            statusLabel.setText("");
        }
    }

    private void renderCredentials(ClientView view) {
        credentialsBox.getChildren().clear();
        if (view.credentials().isEmpty()) {
            credentialsBox.getChildren().add(hint("Sin credenciales."));
            return;
        }
        view.credentials().forEach(credential -> credentialsBox.getChildren().add(credentialCard(credential)));
    }

    private HBox credentialCard(CredentialView credential) {
        Label type = new Label(credential.typeLabel());
        type.getStyleClass().add("credential-type");
        Label code = new Label(credential.code());
        code.getStyleClass().add("credential-code");
        Label meta = new Label(credentialMeta(credential));
        meta.getStyleClass().add("muted");
        VBox text = new VBox(2, type, code, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label status = new Label(credential.statusLabel());
        status.getStyleClass().add("VIGENTE".equals(credential.statusLabel()) ? "badge-ready" : "badge-soon");
        HBox row = new HBox(10, text, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("credential-card");
        return row;
    }

    private String credentialMeta(CredentialView credential) {
        if (credential.expiresAt() == null) {
            return "Sin vencimiento";
        }
        return "Vence " + DATE_FORMAT.format(credential.expiresAt().toLocalDate());
    }

    private void updateCredentialButtons(ClientView view) {
        boolean enabled = canManage && !selectedInactive;
        boolean hasUsableCard = view.credentials().stream()
                .anyMatch(item -> item.type() == CredentialType.CARD && "VIGENTE".equals(item.statusLabel()));
        boolean hasAnyCard = view.credentials().stream()
                .anyMatch(item -> item.type() == CredentialType.CARD);
        boolean hasUsableQr = view.credentials().stream()
                .anyMatch(item -> item.type() == CredentialType.QR && "VIGENTE".equals(item.statusLabel()));
        issueCardButton.setDisable(!enabled || hasUsableCard);
        renewCardButton.setDisable(!enabled || !hasAnyCard);
        issueQrButton.setDisable(!enabled || hasUsableQr);
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

    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        return label;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
