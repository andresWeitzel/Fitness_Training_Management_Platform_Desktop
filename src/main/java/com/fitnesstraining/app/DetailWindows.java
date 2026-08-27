package com.fitnesstraining.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Objects;

/** Ventanas de detalle flotantes (tarjeta opaca sobre el módulo dueño). */
public final class DetailWindows {

    private static final Color SCRIM = Color.rgb(15, 23, 36, 0.58);
    private static final Color CARD = Color.WHITE;
    private static final CornerRadii CARD_RADIUS = new CornerRadii(18);

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

        hardenOpaqueSurfaces(root);

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
                hardenOpaqueSurfaces(root);
                root.applyCss();
                root.layout();
                stage.requestFocus();
            });
        } else {
            stage.show();
            Platform.runLater(() -> {
                hardenOpaqueSurfaces(root);
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

    /** En stages transparentes el CSS a veces no pinta fondos: se fuerzan por API. */
    private static void hardenOpaqueSurfaces(Parent root) {
        if (root instanceof Region frame && root.getStyleClass().contains("detail-window-frame")) {
            frame.setBackground(new Background(new BackgroundFill(SCRIM, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        Node card = root.lookup(".detail-window");
        if (card instanceof Region region) {
            region.setBackground(new Background(new BackgroundFill(CARD, CARD_RADIUS, Insets.EMPTY)));
            // Evita que max-height 88% + stretch deje la card “hueca”
            if (region.getMaxHeight() == Double.MAX_VALUE || region.getMaxHeight() > 900) {
                region.setMaxHeight(Region.USE_PREF_SIZE);
            }
        }
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
