package com.fitnesstraining.app;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class WindowChrome {

    private WindowChrome() {
    }

    public static void prepare(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
    }

    public static void applyTransparentScene(Scene scene) {
        scene.setFill(Color.TRANSPARENT);
    }

    public static void makeDraggable(Stage stage, Node handle) {
        if (handle == null) {
            return;
        }
        final Delta delta = new Delta();
        handle.setOnMousePressed(event -> {
            if (stage.isMaximized()) {
                return;
            }
            delta.x = event.getScreenX() - stage.getX();
            delta.y = event.getScreenY() - stage.getY();
        });
        handle.setOnMouseDragged(event -> {
            if (stage.isMaximized()) {
                return;
            }
            stage.setX(event.getScreenX() - delta.x);
            stage.setY(event.getScreenY() - delta.y);
        });
    }

    private static final class Delta {
        private double x;
        private double y;
    }
}
