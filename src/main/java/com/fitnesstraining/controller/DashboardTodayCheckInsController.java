package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.checkin.dto.CheckInSummary;
import com.fitnesstraining.checkin.model.AccessMode;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardTodayCheckInsController {

    private static final double CARD_WIDTH = 540;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private StackPane rootPane;
    @FXML private Label countBadge;
    @FXML private Label summaryLabel;
    @FXML private Label showingLabel;
    @FXML private Label helpLabel;
    @FXML private VBox itemsBox;
    @FXML private Button openCheckInButton;

    private Stage stage;
    private AppContext appContext;
    private Runnable onOpenCheckIn;
    private boolean canManageCheckIn;

    public static void open(
            Window owner,
            List<CheckInSummary> today,
            AppContext appContext,
            boolean canManageCheckIn,
            Runnable onOpenCheckIn) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DashboardTodayCheckInsController.class.getResource("/views/dashboard-today-checkins.fxml"));
            Parent root = loader.load();
            DashboardTodayCheckInsController controller = loader.getController();
            controller.appContext = appContext;
            controller.onOpenCheckIn = onOpenCheckIn;
            controller.canManageCheckIn = canManageCheckIn;
            controller.bind(today);
            controller.stage = DetailWindows.open(owner, root, "Recepción de hoy", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir la recepción de hoy.", ex);
        }
    }

    private void bind(List<CheckInSummary> today) {
        openCheckInButton.setVisible(canManageCheckIn);
        openCheckInButton.setManaged(canManageCheckIn);
        itemsBox.getChildren().clear();

        if (today == null || today.isEmpty()) {
            countBadge.setText("0");
            showingLabel.setText("0 ingresos");
            summaryLabel.setText("Todavía no hay ingresos registrados hoy.");
            helpLabel.setText("Cuando se registre un check-in, va a aparecer acá.");
            Label empty = new Label("Sin ingresos en el día.");
            empty.getStyleClass().add("muted");
            itemsBox.getChildren().add(empty);
            return;
        }

        countBadge.setText(String.valueOf(today.size()));
        showingLabel.setText(today.size() + (today.size() == 1 ? " ingreso" : " ingresos"));
        summaryLabel.setText("Listado de ingresos del predio en el día de hoy.");
        helpLabel.setText(canManageCheckIn
                ? "Tocá Ir a recepción para operar el módulo completo."
                : "Consulta de ingresos del día.");

        for (CheckInSummary row : today) {
            itemsBox.getChildren().add(row(row));
        }
    }

    private HBox row(CheckInSummary item) {
        Label name = new Label(item.clientName());
        name.getStyleClass().add("activity-name");
        Label meta = new Label(item.clientDocument()
                + " · "
                + TIME_FORMAT.format(item.checkedInAt().toLocalTime())
                + " · "
                + labelForMode(item.accessMode()));
        meta.getStyleClass().add("muted");
        VBox text = new VBox(2, name, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("Ingreso");
        badge.getStyleClass().add("badge-ready");
        HBox row = new HBox(12, text, spacer, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        row.getStyleClass().add("activity-row-disabled");
        row.setCursor(javafx.scene.Cursor.DEFAULT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static String labelForMode(AccessMode mode) {
        if (mode == null) {
            return "Acceso";
        }
        return switch (mode) {
            case MEMBERSHIP -> "Membresía";
            case DAILY_PASS -> "Pase diario";
        };
    }

    @FXML
    public void onOpenCheckIn() {
        onClose();
        if (onOpenCheckIn != null) {
            onOpenCheckIn.run();
        } else if (appContext != null) {
            appContext.openModule("checkin");
        }
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
