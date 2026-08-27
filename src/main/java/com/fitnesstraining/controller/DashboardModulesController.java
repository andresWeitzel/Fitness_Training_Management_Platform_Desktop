package com.fitnesstraining.controller;

import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.app.NavItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class DashboardModulesController {

    private static final double CARD_WIDTH = 540;

    @FXML private StackPane rootPane;
    @FXML private Label countBadge;
    @FXML private Label summaryLabel;
    @FXML private Label readyLabel;
    @FXML private Label upcomingLabel;
    @FXML private VBox itemsBox;

    private Stage stage;
    private AppContext appContext;

    public static void open(Window owner, List<NavItem> modules, AppContext appContext) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DashboardModulesController.class.getResource("/views/dashboard-modules.fxml"));
            Parent root = loader.load();
            DashboardModulesController controller = loader.getController();
            controller.appContext = appContext;
            controller.bind(modules);
            controller.stage = DetailWindows.open(owner, root, "Estado de módulos", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el estado de módulos.", ex);
        }
    }

    private void bind(List<NavItem> modules) {
        itemsBox.getChildren().clear();
        if (modules == null || modules.isEmpty()) {
            countBadge.setText("0");
            readyLabel.setText("0");
            upcomingLabel.setText("0");
            summaryLabel.setText("No hay módulos visibles para tu rol.");
            Label empty = new Label("Sin módulos para mostrar.");
            empty.getStyleClass().add("muted");
            itemsBox.getChildren().add(empty);
            return;
        }

        List<NavItem> ordered = modules.stream()
                .sorted(Comparator
                        .comparing(NavItem::implemented).reversed()
                        .thenComparing(NavItem::group)
                        .thenComparing(NavItem::label))
                .toList();

        long ready = ordered.stream().filter(NavItem::implemented).count();
        long upcoming = ordered.size() - ready;
        countBadge.setText(String.valueOf(ordered.size()));
        readyLabel.setText(String.valueOf(ready));
        upcomingLabel.setText(String.valueOf(upcoming));
        summaryLabel.setText("Módulos disponibles según tu permiso de acceso.");

        ordered.forEach(item -> itemsBox.getChildren().add(row(item)));
    }

    private HBox row(NavItem item) {
        Label name = new Label(item.label());
        name.getStyleClass().add("activity-name");
        Label group = new Label(item.group());
        group.getStyleClass().add("muted");
        VBox text = new VBox(2, name, group);
        HBox.setHgrow(text, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label(item.implemented() ? "Listo" : "Próximo");
        badge.getStyleClass().add(item.implemented() ? "badge-ready" : "badge-soon");
        HBox row = new HBox(12, text, spacer, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        row.setMaxWidth(Double.MAX_VALUE);
        if (item.implemented()) {
            row.setOnMouseClicked(event -> {
                onClose();
                if (appContext != null) {
                    appContext.openModule(item.id());
                }
            });
        } else {
            row.setCursor(javafx.scene.Cursor.DEFAULT);
            row.getStyleClass().add("activity-row-disabled");
        }
        return row;
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
