package com.fitnesstraining.app;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;

public final class ConfirmDialogs {

    private ConfirmDialogs() {
    }

    public static boolean confirm(Node owner, String title, String message) {
        return confirm(owner, title, title, message);
    }

    public static boolean confirm(Node owner, String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title == null || title.isBlank() ? "Confirmar" : title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        ButtonType accept = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, cancel);

        Window window = resolveWindow(owner);
        if (window != null) {
            alert.initOwner(window);
        }

        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("app-confirm-dialog");
        String stylesheet = Objects.requireNonNull(
                ConfirmDialogs.class.getResource("/css/app.css"),
                "No se encontró /css/app.css").toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == accept;
    }

    private static Window resolveWindow(Node owner) {
        if (owner == null || owner.getScene() == null) {
            return null;
        }
        return owner.getScene().getWindow();
    }
}
