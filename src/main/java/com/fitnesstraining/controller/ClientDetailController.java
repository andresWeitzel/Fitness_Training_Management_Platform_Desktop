package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.service.ClientService;
import com.fitnesstraining.memberships.model.MembershipStatus;
import com.fitnesstraining.shared.exception.ValidationException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ClientDetailController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final double CARD_WIDTH = 500;

    @FXML private StackPane rootPane;
    @FXML private Label statusBadge;
    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label opsPlanLabel;
    @FXML private Label opsDebtLabel;
    @FXML private Label opsCheckInLabel;
    @FXML private Label opsRoutineLabel;
    @FXML private Label opsAssessmentLabel;
    @FXML private Label opsNutritionLabel;
    @FXML private VBox credentialsBox;
    @FXML private HBox credentialActionsBox;
    @FXML private Button issueCardButton;
    @FXML private Button renewCardButton;
    @FXML private Button issueQrButton;
    @FXML private Label statusLabel;

    private Stage stage;
    private ClientService clientService;
    private boolean canManage;
    private Consumer<ClientView> onChanged;
    private ClientView current;

    public static void open(
            Window owner,
            ClientView view,
            ClientService clientService,
            boolean canManage,
            Consumer<ClientView> onChanged) {
        if (view == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    ClientDetailController.class.getResource("/views/client-detail.fxml"));
            Parent root = loader.load();
            ClientDetailController controller = loader.getController();
            controller.clientService = clientService;
            controller.canManage = canManage;
            controller.onChanged = onChanged;
            controller.bind(view);
            controller.stage = DetailWindows.open(owner, root, "Detalle del cliente", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle del cliente.", ex);
        }
    }

    private void bind(ClientView view) {
        this.current = view;
        boolean inactive = view.status() != ClientStatus.ACTIVE;

        titleLabel.setText(view.lastName() + ", " + view.firstName());
        statusBadge.setText(inactive ? "Baja" : "Activo");
        statusBadge.getStyleClass().setAll(inactive ? "badge-soon" : "badge-ready");

        String number = view.credentials().stream()
                .filter(item -> item.type() == CredentialType.CLIENT_NUMBER)
                .map(CredentialView::code)
                .findFirst()
                .orElse("sin n°");
        summaryLabel.setText(view.documentNumber() + " · " + number
                + (view.email() == null || view.email().isBlank() ? "" : " · " + view.email()));

        renderOps(view);
        renderCredentials(view.credentials());
        updateCredentialButtons(view);
        credentialActionsBox.setVisible(canManage);
        credentialActionsBox.setManaged(canManage);

        if (inactive) {
            statusLabel.setText("Cliente dado de baja. Solo consulta de resumen y credenciales.");
        } else {
            statusLabel.setText("Hacé click en una credencial o en Copiar para llevarla al portapapeles.");
        }
    }

    private void renderOps(ClientView view) {
        if (view.membershipPlanName() == null || view.membershipPlanName().isBlank()) {
            opsPlanLabel.setText("Sin plan activo");
        } else {
            String ends = view.membershipEndsOn() == null ? "—" : DATE_FORMAT.format(view.membershipEndsOn());
            String status = view.membershipStatus() == null ? "" : " · " + labelForMembership(view.membershipStatus());
            opsPlanLabel.setText(view.membershipPlanName() + " · vence " + ends + status);
        }
        opsDebtLabel.setText(view.hasBlockingDebt() ? "Mora / recargo pendiente" : "Al día");
        opsDebtLabel.getStyleClass().setAll(view.hasBlockingDebt() ? "status-error" : "preview-value");
        opsCheckInLabel.setText(view.lastCheckInAt() == null
                ? "Sin registros"
                : DATE_TIME_FORMAT.format(view.lastCheckInAt()));
        if (view.activeRoutineTitle() == null || view.activeRoutineTitle().isBlank()) {
            opsRoutineLabel.setText("Sin rutina activa");
        } else if (view.activeRoutineFocus() == null || view.activeRoutineFocus().isBlank()) {
            opsRoutineLabel.setText(view.activeRoutineTitle());
        } else {
            opsRoutineLabel.setText(view.activeRoutineTitle() + " · " + view.activeRoutineFocus());
        }
        opsAssessmentLabel.setText(view.lastAssessmentSummary() == null || view.lastAssessmentSummary().isBlank()
                ? "Sin evaluaciones"
                : view.lastAssessmentSummary());
        opsNutritionLabel.setText(view.activeNutritionPlanTitle() == null || view.activeNutritionPlanTitle().isBlank()
                ? "Sin plan activo"
                : view.activeNutritionPlanTitle());
    }

    private void renderCredentials(java.util.List<CredentialView> credentials) {
        credentialsBox.getChildren().clear();
        if (credentials == null || credentials.isEmpty()) {
            Label empty = new Label("Sin credenciales emitidas.");
            empty.getStyleClass().add("muted");
            credentialsBox.getChildren().add(empty);
            return;
        }
        credentials.forEach(credential -> credentialsBox.getChildren().add(credentialCard(credential)));
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
        statusLabel.setText(credential.typeLabel() + " copiado: " + credential.code());
    }

    private void updateCredentialButtons(ClientView view) {
        boolean enabled = canManage && view.status() == ClientStatus.ACTIVE;
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

    @FXML
    public void onIssueCard() {
        runCredential(() -> clientService.issueCard(current.id()), "Carnet emitido.");
    }

    @FXML
    public void onRenewCard() {
        runCredential(() -> clientService.renewCard(current.id()), "Carnet renovado por 12 meses.");
    }

    @FXML
    public void onIssueQr() {
        runCredential(() -> clientService.issueQr(current.id()), "Código QR generado.");
    }

    private void runCredential(java.util.function.Supplier<ClientView> action, String success) {
        try {
            ClientView updated = action.get();
            bind(updated);
            statusLabel.setText(success);
            if (onChanged != null) {
                onChanged.accept(updated);
            }
        } catch (ValidationException ex) {
            statusLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private static String labelForMembership(MembershipStatus status) {
        return switch (status) {
            case ACTIVE -> "Activa";
            case EXPIRED -> "Vencida";
            case CANCELLED -> "Cancelada";
        };
    }
}
