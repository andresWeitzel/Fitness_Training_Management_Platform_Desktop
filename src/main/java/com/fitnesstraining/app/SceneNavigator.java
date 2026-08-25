package com.fitnesstraining.app;

import com.fitnesstraining.controller.DbSetupController;
import com.fitnesstraining.controller.PlaceholderController;
import com.fitnesstraining.controller.ShellController;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class SceneNavigator {

    private final Stage stage;
    private final ViewLoader views;

    public SceneNavigator(Stage stage, ViewLoader views) {
        this.stage = stage;
        this.views = views;
    }

    public ViewLoader views() {
        return views;
    }

    public void showDemoAccounts() {
        var loaded = views.load("/views/demo-accounts.fxml");
        replaceScene(loaded.root(), 520, 620, false);
        WindowChrome.fitStage(loaded.root());
    }

    public void showDbSetup(String errorMessage) {
        var loaded = views.load("/views/db-setup.fxml");
        DbSetupController controller = (DbSetupController) loaded.controller();
        controller.prepare(DbSetupMode.INSTALL, errorMessage);
        replaceScene(loaded.root(), 520, 560, false);
        WindowChrome.fitStage(loaded.root());
    }

    public void showLogin() {
        var loaded = views.load("/views/login.fxml");
        replaceScene(loaded.root(), 520, 480, false);
        WindowChrome.fitStage(loaded.root());
    }

    public void showShell() {
        var loaded = views.load("/views/shell.fxml");
        replaceScene(loaded.root(), 1480, 900, true);
        ((ShellController) loaded.controller()).showHome();
    }

    public Parent loadPlaceholder(String title, String description) {
        var loaded = views.<PlaceholderController>load("/views/placeholder.fxml");
        loaded.controller().show(title, description);
        return loaded.root();
    }

    public Parent loadModuleError(String title, String message) {
        var loaded = views.<PlaceholderController>load("/views/placeholder.fxml");
        loaded.controller().showError(title, message);
        return loaded.root();
    }

    /**
     * Cambia de pantalla sin animar un resize visible: oculta el stage,
     * monta la nueva escena ya en su tamaño final y vuelve a mostrar.
     */
    private void replaceScene(Parent root, int width, int height, boolean shell) {
        boolean wasShowing = stage.isShowing();
        if (wasShowing) {
            stage.hide();
        }

        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, width, height);
            scene.getStylesheets().add(css());
            stage.setScene(scene);
        } else {
            if (!scene.getStylesheets().contains(css())) {
                scene.getStylesheets().add(css());
            }
            scene.setRoot(root);
        }
        WindowChrome.applyTransparentScene(scene);
        applyWindowBounds(root, width, height, shell);
        installDrag(root);

        if (shell) {
            stage.centerOnScreen();
            WindowChrome.rememberShellBounds(stage);
        }
        stage.setOpacity(1);
        if (wasShowing || !stage.isShowing()) {
            stage.show();
        }
    }

    private void applyWindowBounds(Parent root, int width, int height, boolean shell) {
        stage.setMaximized(false);
        root.getStyleClass().remove("maximized");
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        if (shell) {
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setMaxWidth(Double.MAX_VALUE);
            stage.setMaxHeight(Double.MAX_VALUE);
            stage.setWidth(Math.min(width, bounds.getWidth() - 32));
            stage.setHeight(Math.min(height, bounds.getHeight() - 32));
        } else {
            stage.setMinWidth(420);
            stage.setMinHeight(380);
            stage.setMaxWidth(bounds.getWidth() - 24);
            stage.setMaxHeight(bounds.getHeight() - 24);
            stage.setWidth(width);
            stage.setHeight(height);
        }
    }

    private void installDrag(Parent root) {
        Node dragHandle = lookup(root, ".window-drag");
        Node sidebarDrag = lookup(root, ".sidebar-drag");
        if (sidebarDrag != null && sidebarDrag != dragHandle) {
            WindowChrome.makeDraggable(stage, dragHandle, sidebarDrag);
        } else {
            WindowChrome.makeDraggable(stage, dragHandle);
        }
    }

    private static Node lookup(Parent root, String selector) {
        Node node = root.lookup(selector);
        return node == null ? root : node;
    }

    private static String css() {
        return SceneNavigator.class.getResource("/css/app.css").toExternalForm();
    }
}
