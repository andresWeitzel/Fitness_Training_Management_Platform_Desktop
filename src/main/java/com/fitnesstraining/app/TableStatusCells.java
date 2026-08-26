package com.fitnesstraining.app;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.StackPane;

import java.util.function.BiFunction;

/** Badges compactos y centrados en columnas de estado de tablas. */
public final class TableStatusCells {

    private TableStatusCells() {
    }

    public static <T> TableCell<T, String> of(BiFunction<T, String, String> badgeClassFn) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableView() == null
                        || getIndex() < 0
                        || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                @SuppressWarnings("unchecked")
                T row = (T) getTableView().getItems().get(getIndex());
                Label badge = new Label(item);
                String badgeClass = badgeClassFn.apply(row, item);
                if (badgeClass == null || badgeClass.isBlank()) {
                    badge.getStyleClass().add("table-status-badge");
                } else {
                    badge.getStyleClass().addAll("table-status-badge", badgeClass);
                }
                StackPane holder = new StackPane(badge);
                holder.setAlignment(Pos.CENTER);
                holder.setMaxWidth(Double.MAX_VALUE);
                setGraphic(holder);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }
}
