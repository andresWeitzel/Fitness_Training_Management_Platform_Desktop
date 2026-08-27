package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.model.ClientStatus;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;

public class DashboardRecentActivityController {

    private static final double CARD_WIDTH = 540;

    @FXML private StackPane rootPane;
    @FXML private Label countBadge;
    @FXML private Label summaryLabel;
    @FXML private Label showingLabel;
    @FXML private Label helpLabel;
    @FXML private VBox itemsBox;
    @FXML private Button openClientsButton;

    private Stage stage;
    private AppContext appContext;
    private Runnable onOpenClients;

    public static void open(
            Window owner,
            List<ClientSummary> recentClients,
            AppContext appContext,
            boolean canViewClients,
            Runnable onOpenClients) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DashboardRecentActivityController.class.getResource("/views/dashboard-recent-activity.fxml"));
            Parent root = loader.load();
            DashboardRecentActivityController controller = loader.getController();
            controller.appContext = appContext;
            controller.onOpenClients = onOpenClients;
            controller.bind(recentClients, canViewClients);
            controller.stage = DetailWindows.open(owner, root, "Actividad reciente", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir la actividad reciente.", ex);
        }
    }

    private void bind(List<ClientSummary> recentClients, boolean canViewClients) {
        openClientsButton.setVisible(canViewClients);
        openClientsButton.setManaged(canViewClients);
        itemsBox.getChildren().clear();

        if (recentClients == null || recentClients.isEmpty()) {
            countBadge.setText("0");
            showingLabel.setText("0 altas");
            summaryLabel.setText("Todavía no hay altas recientes para mostrar.");
            helpLabel.setText("Cuando registres clientes, aparecerán acá.");
            Label empty = new Label("Sin actividad reciente.");
            empty.getStyleClass().add("muted");
            itemsBox.getChildren().add(empty);
            return;
        }

        countBadge.setText(String.valueOf(recentClients.size()));
        showingLabel.setText(recentClients.size() + (recentClients.size() == 1 ? " alta" : " altas"));
        summaryLabel.setText("Listado de las últimas altas del gimnasio.");
        helpLabel.setText(canViewClients
                ? "Tocá una fila para ir al módulo Clientes."
                : "Consulta de altas recientes.");

        for (ClientSummary client : recentClients) {
            itemsBox.getChildren().add(row(client, canViewClients));
        }
    }

    private HBox row(ClientSummary client, boolean canViewClients) {
        Label name = new Label(client.fullName());
        name.getStyleClass().add("activity-name");
        Label meta = new Label(client.documentNumber()
                + " · "
                + (client.clientNumber() == null || client.clientNumber().isBlank() ? "sin n°" : "N° " + client.clientNumber()));
        meta.getStyleClass().add("muted");
        VBox text = new VBox(2, name, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label status = new Label(client.status() == ClientStatus.ACTIVE ? "Activo" : "Baja");
        status.getStyleClass().add(client.status() == ClientStatus.ACTIVE ? "badge-ready" : "badge-soon");
        HBox row = new HBox(12, text, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        row.setMaxWidth(Double.MAX_VALUE);
        if (canViewClients) {
            row.setOnMouseClicked(event -> goToClients());
        } else {
            row.setCursor(javafx.scene.Cursor.DEFAULT);
            row.getStyleClass().add("activity-row-disabled");
        }
        return row;
    }

    @FXML
    public void onOpenClients() {
        goToClients();
    }

    private void goToClients() {
        onClose();
        if (onOpenClients != null) {
            onOpenClients.run();
        } else if (appContext != null) {
            appContext.openModule("clients");
        }
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
