package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.checkin.dto.CheckInDetail;
import com.fitnesstraining.checkin.model.AccessMode;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.CredentialType;
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
import java.util.List;

public class CheckInDetailController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double CARD_WIDTH = 460;

    @FXML private StackPane rootPane;
    @FXML private Label modeBadge;
    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label clientLabel;
    @FXML private Label contactLabel;
    @FXML private Label modeLabel;
    @FXML private Label usedLabel;
    @FXML private VBox credentialsBox;
    @FXML private Label statusLabel;

    private Stage stage;

    public static void open(Window owner, CheckInDetail detail) {
        if (detail == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    CheckInDetailController.class.getResource("/views/checkin-detail.fxml"));
            Parent root = loader.load();
            CheckInDetailController controller = loader.getController();
            controller.bind(detail);
            controller.stage = DetailWindows.open(owner, root, "Detalle del ingreso", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el detalle del ingreso.", ex);
        }
    }

    private void bind(CheckInDetail detail) {
        titleLabel.setText(detail.clientName());
        modeBadge.setText(labelForMode(detail.accessMode()));
        modeBadge.getStyleClass().setAll(
                detail.accessMode() == AccessMode.MEMBERSHIP ? "badge-paid" : "badge-pending");
        summaryLabel.setText("Ingreso #" + detail.checkInId() + " · " + formatDateTime(detail.checkedInAt())
                + (detail.notes() == null || detail.notes().isBlank() ? "" : " · " + detail.notes()));
        clientLabel.setText(detail.clientName() + " · " + detail.clientDocument()
                + (detail.clientNumber() == null ? "" : " · " + detail.clientNumber()));
        contactLabel.setText(joinContact(detail.clientEmail(), detail.clientPhone()));
        modeLabel.setText(labelForMode(detail.accessMode())
                + (detail.membershipPlanName() == null ? "" : " · " + detail.membershipPlanName()));
        usedLabel.setText(formatCredential(detail.usedCredentialType(), detail.usedCredentialCode()));
        renderCredentials(detail.credentials());
        statusLabel.setText("Hacé click en una credencial o en Copiar para llevarla al portapapeles.");
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void renderCredentials(List<CredentialView> credentials) {
        credentialsBox.getChildren().clear();
        if (credentials == null || credentials.isEmpty()) {
            Label empty = new Label("Sin credenciales emitidas.");
            empty.getStyleClass().add("muted");
            credentialsBox.getChildren().add(empty);
            return;
        }
        for (CredentialView credential : credentials) {
            credentialsBox.getChildren().add(credentialCard(credential));
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
        statusLabel.setText(credential.typeLabel() + " copiado: " + credential.code());
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

    private static String formatCredential(CredentialType type, String code) {
        if (code == null || code.isBlank()) {
            return type == null ? "Documento" : labelForCredentialType(type);
        }
        return labelForCredentialType(type) + " · " + code;
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

    private static String formatDateTime(java.time.OffsetDateTime value) {
        return value == null ? "—" : DATE_TIME_FORMAT.format(value);
    }
}
