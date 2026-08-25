package com.fitnesstraining.app;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        stage.show();

        Platform.runLater(() -> {
            root.applyCss();
            root.layout();
            double width = Math.ceil(root.prefWidth(-1));
            double height = Math.ceil(root.prefHeight(preferredCardWidth));
            stage.setWidth(width);
            stage.setHeight(height);
            if (owner != null) {
                double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
                double y = owner.getY() + Math.max(56, (owner.getHeight() - stage.getHeight()) / 3);
                stage.setX(Math.max(owner.getX() + 12, x));
                stage.setY(Math.max(owner.getY() + 12, y));
            } else {
                stage.centerOnScreen();
            }
            stage.requestFocus();
        });
        return stage;
    }
}
