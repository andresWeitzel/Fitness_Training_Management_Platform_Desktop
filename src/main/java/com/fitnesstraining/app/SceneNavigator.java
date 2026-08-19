package com.fitnesstraining.app;

import com.fitnesstraining.controller.DbSetupController;
import com.fitnesstraining.controller.PlaceholderController;
import com.fitnesstraining.controller.ShellController;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    public void showDbSetup(String errorMessage) {
        var loaded = views.load("/views/db-setup.fxml");
        DbSetupController controller = (DbSetupController) loaded.controller();
        controller.prepare(errorMessage);
        setScene(loaded.root(), 880, 780, false);
    }

    public void showLogin() {
        setScene(views.load("/views/login.fxml").root(), 960, 720, false);
    }

    public void showShell() {
        var loaded = views.load("/views/shell.fxml");
        setScene(loaded.root(), 1480, 900, true);
        ((ShellController) loaded.controller()).showHome();
    }

    public Parent loadPlaceholder(String title, String description) {
        var loaded = views.<PlaceholderController>load("/views/placeholder.fxml");
        loaded.controller().show(title, description);
        return loaded.root();
    }

    private void setScene(Parent root, int width, int height, boolean maximized) {
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
        stage.setMaximized(false);
        if (maximized) {
            stage.setMinWidth(1200);
            stage.setMinHeight(720);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setMaximized(true);
        } else {
            stage.setMinWidth(880);
            stage.setMinHeight(640);
            stage.setWidth(width);
            stage.setHeight(height);
        }
    }

    private static String css() {
        return SceneNavigator.class.getResource("/css/app.css").toExternalForm();
    }
}
