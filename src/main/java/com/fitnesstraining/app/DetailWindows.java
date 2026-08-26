package com.fitnesstraining.app;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Objects;

/** Ventanas de detalle flotantes (tarjeta opaca sobre el módulo dueño). */
public final class DetailWindows {

    private DetailWindows() {
    }

    public static Stage open(Window owner, Parent root, String title, double preferredCardWidth) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);
        stage.setTitle(title == null ? "Detalle" : title);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(
                DetailWindows.class.getResource("/css/app.css")).toExternalForm());
        stage.setScene(scene);
        WindowChrome.makeDraggable(stage, root.lookup(".window-drag"));

        if (owner != null) {
            alignOverlayStage(stage, owner, root);
            stage.show();
            Platform.runLater(() -> {
                root.applyCss();
                root.layout();
                stage.requestFocus();
            });
        } else {
            stage.show();
            Platform.runLater(() -> {
                root.applyCss();
                root.layout();
                double width = Math.max(Math.ceil(root.prefWidth(-1)), preferredCardWidth);
                stage.setWidth(width);
                stage.setHeight(Math.ceil(root.prefHeight(width)));
                stage.centerOnScreen();
                stage.requestFocus();
            });
        }
        return stage;
    }

    private static void alignOverlayStage(Stage stage, Window owner, Parent root) {
        stage.setWidth(owner.getWidth());
        stage.setHeight(owner.getHeight());
        stage.setX(owner.getX());
        stage.setY(owner.getY());
        if (root instanceof Region region) {
            region.prefWidthProperty().bind(stage.widthProperty());
            region.prefHeightProperty().bind(stage.heightProperty());
            region.minWidthProperty().bind(stage.widthProperty());
            region.minHeightProperty().bind(stage.heightProperty());
            region.maxWidthProperty().bind(stage.widthProperty());
            region.maxHeightProperty().bind(stage.heightProperty());
        }
    }
}
