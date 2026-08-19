package com.fitnesstraining.controller;

import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.service.ClientService;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ClientsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private TextField searchField;
    @FXML private TableView<ClientSummary> clientsTable;
    @FXML private TableColumn<ClientSummary, String> documentColumn;
    @FXML private TableColumn<ClientSummary, String> nameColumn;
    @FXML private TableColumn<ClientSummary, String> numberColumn;
    @FXML private TableColumn<ClientSummary, String> statusColumn;

    @FXML private TextField documentField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextArea credentialsArea;
    @FXML private Label statusLabel;

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
        numberColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientNumber()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));

        clientsTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            if (selected != null) {
                loadClient(selected.id());
            }
        });
        searchField.setOnAction(event -> onSearch());
        reloadTable("");
        onNew();
    }

    @FXML
    public void onSearch() {
        reloadTable(searchField.getText());
    }

    @FXML
    public void onNew() {
        selectedId = null;
        clientsTable.getSelectionModel().clearSelection();
        documentField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressField.setText("");
        documentField.setDisable(!canManage);
        credentialsArea.setText("Al guardar se genera el número de cliente. Después se puede emitir carnet y QR.");
        statusLabel.setText("");
        statusLabel.getStyleClass().setAll("muted");
        deactivateButton.setDisable(true);
        issueCardButton.setDisable(true);
        renewCardButton.setDisable(true);
        issueQrButton.setDisable(true);
        if (canManage) {
            saveButton.setDisable(false);
        }
    }

    @FXML
    public void onSave() {
        try {
            ClientRequest request = readForm();
            ClientView saved = selectedId == null
                    ? clientService.create(request)
                    : clientService.update(selectedId, request);
            statusOk(selectedId == null ? "Cliente registrado." : "Cambios guardados.");
            reloadTable(searchField.getText());
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
            reloadTable(searchField.getText());
            onNew();
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

    private void runCredential(java.util.function.Supplier<ClientView> action, String success) {
        if (selectedId == null) {
            statusError("Seleccione o guarde un cliente primero.");
            return;
        }
        try {
            ClientView view = action.get();
            statusOk(success);
            show(view);
            reloadTable(searchField.getText());
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
        documentField.setText(view.documentNumber());
        firstNameField.setText(view.firstName());
        lastNameField.setText(view.lastName());
        emailField.setText(nullToEmpty(view.email()));
        phoneField.setText(nullToEmpty(view.phone()));
        addressField.setText(nullToEmpty(view.address()));
        documentField.setDisable(true);
        credentialsArea.setText(formatCredentials(view));
        deactivateButton.setDisable(!canManage);
        issueCardButton.setDisable(!canManage);
        renewCardButton.setDisable(!canManage);
        issueQrButton.setDisable(!canManage);
        statusLabel.setText("");
    }

    private String formatCredentials(ClientView view) {
        if (view.credentials().isEmpty()) {
            return "Sin credenciales.";
        }
        StringBuilder builder = new StringBuilder();
        for (CredentialView credential : view.credentials()) {
            builder.append(credential.typeLabel())
                    .append("  ")
                    .append(credential.code())
                    .append("  ·  ")
                    .append(credential.statusLabel());
            if (credential.expiresAt() != null) {
                builder.append("  ·  vence ")
                        .append(DATE_FORMAT.format(credential.expiresAt().toLocalDate()));
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    private void reloadTable(String query) {
        clientsTable.setItems(FXCollections.observableArrayList(clientService.list(query)));
    }

    private void selectById(Long id) {
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
        documentField.setEditable(canManage);
        firstNameField.setEditable(canManage);
        lastNameField.setEditable(canManage);
        emailField.setEditable(canManage);
        phoneField.setEditable(canManage);
        addressField.setEditable(canManage);
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
