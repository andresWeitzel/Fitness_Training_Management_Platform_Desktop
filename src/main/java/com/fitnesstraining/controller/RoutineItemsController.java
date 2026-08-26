package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.controller.TrainingController.RoutineItemRow;
import com.fitnesstraining.training.dto.ExerciseOption;
import com.fitnesstraining.training.service.TrainingService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;

public class RoutineItemsController {

    private static final double CARD_WIDTH = 640;

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private ComboBox<ExerciseOption> itemExerciseCombo;
    @FXML private TextField itemSetsField;
    @FXML private TextField itemRepsField;
    @FXML private TextField itemRestField;
    @FXML private TextField itemLoadField;
    @FXML private Button addItemButton;
    @FXML private TableView<RoutineItemRow> itemsTable;
    @FXML private TableColumn<RoutineItemRow, String> itemNameColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemSetsColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemRepsColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemRestColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemLoadColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemDetailColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemRemoveColumn;
    @FXML private Label statusLabel;

    private Stage stage;
    private ObservableList<RoutineItemRow> items;
    private boolean editable;
    private Runnable onChange;

    public static void open(
            Window owner,
            String routineTitle,
            ObservableList<RoutineItemRow> items,
            TrainingService trainingService,
            boolean editable,
            Runnable onChange) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    RoutineItemsController.class.getResource("/views/routine-items.fxml"));
            Parent root = loader.load();
            RoutineItemsController controller = loader.getController();
            controller.bind(routineTitle, items, trainingService, editable, onChange);
            controller.stage = DetailWindows.open(owner, root, "Ejercicios de la rutina", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir la gestión de ejercicios.", ex);
        }
    }

    private void bind(
            String routineTitle,
            ObservableList<RoutineItemRow> items,
            TrainingService trainingService,
            boolean editable,
            Runnable onChange) {
        this.items = items;
        this.editable = editable;
        this.onChange = onChange;
        titleLabel.setText(routineTitle == null || routineTitle.isBlank() ? "Rutina sin título" : routineTitle);
        updateSummary();
        setupExerciseCombo(trainingService);
        setupTable();
        applyEditableState();
        statusInfo(editable
                ? "Agregue ejercicios con prescripción. Use ⓘ para ver técnica y músculos."
                : "Solo consulta. La rutina está archivada o su rol no puede editarla.");
    }

    @FXML
    public void onAddItem() {
        if (!editable) {
            return;
        }
        ExerciseOption exercise = itemExerciseCombo.getValue();
        if (exercise == null) {
            statusError("Seleccione un ejercicio para agregar.");
            return;
        }
        Integer sets = parsePositiveInt(itemSetsField.getText(), "series");
        Integer rest = parseNonNegativeInt(itemRestField.getText(), "descanso");
        if (sets == null && itemSetsField.getText() != null && !itemSetsField.getText().isBlank()) {
            return;
        }
        if (rest == null && itemRestField.getText() != null && !itemRestField.getText().isBlank()) {
            return;
        }
        items.add(new RoutineItemRow(
                exercise.id(),
                exercise.name(),
                TrainingService.labelForMuscle(exercise.muscleGroup()),
                TrainingService.labelForEquipment(exercise.equipment()),
                TrainingService.labelForDifficulty(exercise.difficulty()),
                exercise.secondaryMuscles(),
                exercise.description(),
                exercise.techniqueNotes(),
                sets,
                blankToNull(itemRepsField.getText()),
                rest,
                blankToNull(itemLoadField.getText())));
        clearDraftFields();
        notifyChange();
        statusOk("Ejercicio agregado.");
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void setupExerciseCombo(TrainingService trainingService) {
        itemExerciseCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                trainingService.listActiveExerciseOptions()));
        itemExerciseCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExerciseOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public ExerciseOption fromString(String string) {
                return null;
            }
        });
        itemExerciseCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ExerciseOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
    }

    private void setupTable() {
        itemsTable.setItems(items);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemNameColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().exerciseName()));
        itemSetsColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().sets() == null ? "—" : String.valueOf(d.getValue().sets())));
        itemRepsColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().reps() == null ? "—" : d.getValue().reps()));
        itemRestColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().restSeconds() == null ? "—" : d.getValue().restSeconds() + " s"));
        itemLoadColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().loadNote() == null ? "—" : d.getValue().loadNote()));

        itemNameColumn.setCellFactory(col -> textCellWithTooltip());
        itemRestColumn.setCellFactory(col -> textCellWithTooltip());
        itemLoadColumn.setCellFactory(col -> textCellWithTooltip());

        itemDetailColumn.setCellFactory(col -> new TableCell<>() {
            private final Button detail = new Button("ⓘ");

            {
                detail.getStyleClass().add("table-icon-button");
                detail.setTooltip(new Tooltip("Ver detalle del ejercicio"));
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < items.size()) {
                        Window owner = itemsTable.getScene() == null ? null : itemsTable.getScene().getWindow();
                        RoutineItemDetailController.open(owner, items.get(getIndex()));
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= items.size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detail);
                setAlignment(Pos.CENTER);
            }
        });
        itemDetailColumn.setSortable(false);

        itemRemoveColumn.setCellFactory(col -> new TableCell<>() {
            private final Button remove = new Button("×");

            {
                remove.getStyleClass().addAll("table-icon-button", "danger");
                remove.setTooltip(new Tooltip("Quitar ejercicio"));
                remove.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < items.size()) {
                        items.remove(getIndex());
                        notifyChange();
                        statusOk("Ejercicio quitado.");
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= items.size()) {
                    setGraphic(null);
                    return;
                }
                remove.setDisable(!editable);
                setGraphic(remove);
                setAlignment(Pos.CENTER);
            }
        });
        itemRemoveColumn.setSortable(false);
    }

    private TableCell<RoutineItemRow, String> textCellWithTooltip() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                setTooltip(new Tooltip(item));
            }
        };
    }

    private void applyEditableState() {
        addItemButton.setDisable(!editable);
        itemExerciseCombo.setDisable(!editable);
        itemSetsField.setDisable(!editable);
        itemRepsField.setDisable(!editable);
        itemRestField.setDisable(!editable);
        itemLoadField.setDisable(!editable);
    }

    private void updateSummary() {
        int count = items == null ? 0 : items.size();
        summaryLabel.setText(count == 0
                ? "Sin ejercicios cargados."
                : count + (count == 1 ? " ejercicio en la rutina." : " ejercicios en la rutina."));
    }

    private void notifyChange() {
        updateSummary();
        if (onChange != null) {
            onChange.run();
        }
    }

    private void clearDraftFields() {
        itemExerciseCombo.setValue(null);
        itemSetsField.clear();
        itemRepsField.clear();
        itemRestField.clear();
        itemLoadField.clear();
    }

    private Integer parsePositiveInt(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed <= 0) {
                statusError("Las " + label + " deben ser mayores a cero.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            statusError("Valor inválido en " + label + ".");
            return null;
        }
    }

    private Integer parseNonNegativeInt(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < 0) {
                statusError("El " + label + " no puede ser negativo.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            statusError("Valor inválido en " + label + ".");
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void statusInfo(String message) {
        statusLabel.getStyleClass().setAll("muted");
        statusLabel.setText(message == null ? "" : message);
    }

    private void statusOk(String message) {
        statusLabel.getStyleClass().setAll("status-ok");
        statusLabel.setText(message);
    }

    private void statusError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message);
    }
}
