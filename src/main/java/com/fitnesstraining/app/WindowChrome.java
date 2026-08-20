package com.fitnesstraining.app;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.atomic.AtomicInteger;

public final class WindowChrome {

    /** Margen para sombras CSS (dropshadow) que sizeToScene no incluye. */
    private static final double SHADOW_MARGIN = 36;
    private static final AtomicInteger fitGeneration = new AtomicInteger();

    private WindowChrome() {
    }

    public static void prepare(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
    }

    public static void applyTransparentScene(Scene scene) {
        scene.setFill(Color.TRANSPARENT);
    }

    public static void fitStage(Node node) {
        if (node == null || node.getScene() == null) {
            return;
        }
        scheduleFit((Stage) node.getScene().getWindow());
    }

    public static void fitStage(Stage stage) {
        scheduleFit(stage);
    }

    private static void scheduleFit(Stage stage) {
        if (stage == null || stage.getScene() == null) {
            return;
        }
        int generation = fitGeneration.incrementAndGet();
        Platform.runLater(() -> Platform.runLater(() -> {
            if (generation != fitGeneration.get() || stage.getScene() == null) {
                return;
            }
            Parent root = stage.getScene().getRoot();
            root.applyCss();
            root.layout();

            double width = measureExtent(root, true);
            double height = measureExtent(root, false);

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setWidth(Math.min(width, screen.getWidth() - 16));
            stage.setHeight(Math.min(height, screen.getHeight() - 16));
            stage.centerOnScreen();
        }));
    }

    private static double measureExtent(Parent root, boolean width) {
        double max = 0;
        double padStart = 0;
        double padEnd = 0;
        if (root instanceof Region region) {
            if (width) {
                padStart = region.getPadding().getLeft();
                padEnd = region.getPadding().getRight();
            } else {
                padStart = region.getPadding().getTop();
                padEnd = region.getPadding().getBottom();
            }
        }
        for (Node child : root.getChildrenUnmodifiable()) {
            if (!child.isManaged() || !child.isVisible()) {
                continue;
            }
            if (width) {
                max = Math.max(max, child.getBoundsInParent().getMaxX());
            } else {
                max = Math.max(max, child.getBoundsInParent().getMaxY());
            }
        }
        if (max <= 0) {
            if (width) {
                max = root.prefWidth(-1);
            } else {
                max = root.prefHeight(-1);
            }
        }
        return Math.ceil(padStart + max + padEnd + SHADOW_MARGIN);
    }

    public static void makeDraggable(Stage stage, Node... handles) {
        if (handles == null) {
            return;
        }
        for (Node handle : handles) {
            if (handle != null) {
                attachDragHandler(stage, handle);
            }
        }
    }

    public static void rememberShellBounds(Stage stage) {
        if (stage != null && !stage.isMaximized()) {
            restoreWidth = stage.getWidth();
            restoreHeight = stage.getHeight();
        }
    }

    private static double restoreWidth = 1480;
    private static double restoreHeight = 900;

    private static void attachDragHandler(Stage stage, Node handle) {
        final Delta delta = new Delta();
        handle.setOnMousePressed(event -> {
            if (stage.isMaximized()) {
                restoreFromMaximized(stage, event.getScreenX(), event.getScreenY());
            }
            delta.x = event.getScreenX() - stage.getX();
            delta.y = event.getScreenY() - stage.getY();
            event.consume();
        });
        handle.setOnMouseDragged(event -> {
            if (stage.isMaximized()) {
                return;
            }
            stage.setX(event.getScreenX() - delta.x);
            stage.setY(event.getScreenY() - delta.y);
            event.consume();
        });
    }

    private static void restoreFromMaximized(Stage stage, double screenX, double screenY) {
        if (stage.getScene() == null) {
            return;
        }
        Parent root = stage.getScene().getRoot();
        root.getStyleClass().remove("maximized");
        stage.setMaximized(false);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(restoreWidth, bounds.getWidth() - 32);
        double height = Math.min(restoreHeight, bounds.getHeight() - 32);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(Math.max(bounds.getMinX(), Math.min(screenX - width * 0.35, bounds.getMaxX() - width)));
        stage.setY(Math.max(bounds.getMinY(), screenY - 28));
    }

    private static final class Delta {
        private double x;
        private double y;
    }
}
