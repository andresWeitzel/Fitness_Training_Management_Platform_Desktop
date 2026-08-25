package com.fitnesstraining.controller;

import com.fitnesstraining.app.ConfirmDialogs;
import com.fitnesstraining.app.SessionContext;
import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.training.dto.ExerciseOption;
import com.fitnesstraining.training.dto.ExerciseRequest;
import com.fitnesstraining.training.dto.ExerciseSummary;
import com.fitnesstraining.training.dto.RoutineItemRequest;
import com.fitnesstraining.training.dto.RoutineRequest;
import com.fitnesstraining.training.dto.RoutineSummary;
import com.fitnesstraining.training.dto.RoutineView;
import com.fitnesstraining.training.dto.TrainingClientOption;
import com.fitnesstraining.training.model.EquipmentType;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;
import com.fitnesstraining.training.model.RoutineListScope;
import com.fitnesstraining.training.model.RoutineStatus;
import com.fitnesstraining.training.service.TrainingService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class TrainingController {

    public record RoutineItemRow(
            Long exerciseId,
            String exerciseName,
            String muscleGroupLabel,
            String equipmentLabel,
            String difficultyLabel,
            String secondaryMuscles,
            String description,
            String techniqueNotes,
            Integer sets,
            String reps,
            Integer restSeconds,
            String loadNote
    ) {
    }

    @FXML private TabPane trainingTabs;

    @FXML private Label routinesCountLabel;
    @FXML private Button filterActiveRoutinesButton;
    @FXML private Button filterDraftRoutinesButton;
    @FXML private Button filterScheduledRoutinesButton;
    @FXML private Button filterArchivedRoutinesButton;
    @FXML private Button filterAllRoutinesButton;
    @FXML private TextField routineSearchField;
    @FXML private Button newRoutineButton;
    @FXML private TableView<RoutineSummary> routinesTable;
    @FXML private TableColumn<RoutineSummary, String> routineClientColumn;
    @FXML private TableColumn<RoutineSummary, String> routineTitleColumn;
    @FXML private TableColumn<RoutineSummary, String> routineFocusColumn;
    @FXML private TableColumn<RoutineSummary, String> routineTrainerColumn;
    @FXML private TableColumn<RoutineSummary, String> routineItemsColumn;
    @FXML private TableColumn<RoutineSummary, String> routineStatusColumn;
    @FXML private Label routineSubtitleLabel;
    @FXML private Label routineStatusBadge;
    @FXML private ComboBox<TrainingClientOption> routineClientCombo;
    @FXML private TextField routineTitleField;
    @FXML private TextField routineFocusField;
    @FXML private ComboBox<RoutineStatus> routineStatusCombo;
    @FXML private DatePicker routineStartsOnPicker;
    @FXML private TextArea routineNotesField;
    @FXML private Button addItemButton;
    @FXML private ComboBox<ExerciseOption> itemExerciseCombo;
    @FXML private TextField itemSetsField;
    @FXML private TextField itemRepsField;
    @FXML private TextField itemRestField;
    @FXML private TextField itemLoadField;
    @FXML private TableView<RoutineItemRow> routineItemsTable;
    @FXML private TableColumn<RoutineItemRow, String> itemNameColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemSetsColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemRepsColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemRestColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemLoadColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemDetailColumn;
    @FXML private TableColumn<RoutineItemRow, String> itemRemoveColumn;
    @FXML private Label routineStatusLabel;
    @FXML private Button saveRoutineButton;
    @FXML private Button archiveRoutineButton;
    @FXML private Button reactivateRoutineButton;

    @FXML private Label exercisesCountLabel;
    @FXML private Button filterActiveExercisesButton;
    @FXML private Button filterInactiveExercisesButton;
    @FXML private Button filterAllExercisesButton;
    @FXML private TextField exerciseSearchField;
    @FXML private Button newExerciseButton;
    @FXML private TableView<ExerciseSummary> exercisesTable;
    @FXML private TableColumn<ExerciseSummary, String> exerciseNameColumn;
    @FXML private TableColumn<ExerciseSummary, String> exerciseGroupColumn;
    @FXML private TableColumn<ExerciseSummary, String> exerciseEquipmentColumn;
    @FXML private TableColumn<ExerciseSummary, String> exerciseDifficultyColumn;
    @FXML private TableColumn<ExerciseSummary, String> exerciseStatusColumn;
    @FXML private Label exerciseSubtitleLabel;
    @FXML private Label exerciseStatusBadge;
    @FXML private TextField exerciseNameField;
    @FXML private ComboBox<MuscleGroup> exerciseGroupCombo;
    @FXML private ComboBox<EquipmentType> exerciseEquipmentCombo;
    @FXML private ComboBox<ExerciseDifficulty> exerciseDifficultyCombo;
    @FXML private TextField exerciseSecondaryField;
    @FXML private TextArea exerciseDescriptionField;
    @FXML private TextArea exerciseTechniqueField;
    @FXML private Label exerciseStatusLabel;
    @FXML private Button saveExerciseButton;
    @FXML private Button deactivateExerciseButton;
    @FXML private Button reactivateExerciseButton;

    private final TrainingService trainingService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;

    private boolean canManage;
    private Long selectedRoutineId;
    private boolean selectedRoutineArchived;
    private Long selectedExerciseId;
    private boolean selectedExerciseInactive;
    private RoutineListScope routineScope = RoutineListScope.ACTIVE;
    private Boolean exerciseActiveOnly = Boolean.TRUE;
    private final ObservableList<RoutineItemRow> draftItems = FXCollections.observableArrayList();
    private final PauseTransition routineSearchDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition exerciseSearchDelay = new PauseTransition(Duration.millis(180));

    public TrainingController(
            TrainingService trainingService,
            SessionContext sessionContext,
            AuthorizationService authorizationService) {
        this.trainingService = trainingService;
        this.sessionContext = sessionContext;
        this.authorizationService = authorizationService;
    }

    @FXML
    public void initialize() {
        AuthenticatedUser user = sessionContext.requireUser();
        canManage = authorizationService.hasPermission(user, PermissionCode.TRAINING_MANAGE);
        setupRoutineTable();
        setupExerciseTable();
        setupCombos();
        setupItemsTable();

        routineSearchDelay.setOnFinished(e -> reloadRoutines());
        routineSearchField.textProperty().addListener((obs, o, v) -> {
            routineSearchDelay.stop();
            routineSearchDelay.playFromStart();
        });
        exerciseSearchDelay.setOnFinished(e -> reloadExercises());
        exerciseSearchField.textProperty().addListener((obs, o, v) -> {
            exerciseSearchDelay.stop();
            exerciseSearchDelay.playFromStart();
        });
        routineStatusCombo.valueProperty().addListener((obs, o, status) ->
                routineStartsOnPicker.setDisable(status != RoutineStatus.SCHEDULED || !canManage || selectedRoutineArchived));

        routinesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                loadRoutine(selected.id());
            }
        });
        exercisesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                loadExercise(selected.id());
            }
        });

        applyPermissions();
        onNewRoutine();
        onNewExercise();
        Platform.runLater(() -> {
            reloadRoutines();
            reloadExercises();
            refreshExerciseOptions();
            refreshClientOptions();
        });
    }

    @FXML public void onFilterActiveRoutines() { setRoutineScope(RoutineListScope.ACTIVE); }
    @FXML public void onFilterDraftRoutines() { setRoutineScope(RoutineListScope.DRAFT); }
    @FXML public void onFilterScheduledRoutines() { setRoutineScope(RoutineListScope.SCHEDULED); }
    @FXML public void onFilterArchivedRoutines() { setRoutineScope(RoutineListScope.ARCHIVED); }
    @FXML public void onFilterAllRoutines() { setRoutineScope(RoutineListScope.ALL); }
    @FXML public void onFilterActiveExercises() { setExerciseFilter(Boolean.TRUE); }
    @FXML public void onFilterInactiveExercises() { setExerciseFilter(Boolean.FALSE); }
    @FXML public void onFilterAllExercises() { setExerciseFilter(null); }

    @FXML
    public void onNewRoutine() {
        selectedRoutineId = null;
        selectedRoutineArchived = false;
        routinesTable.getSelectionModel().clearSelection();
        routineClientCombo.setValue(null);
        routineTitleField.clear();
        routineFocusField.clear();
        routineNotesField.clear();
        routineStatusCombo.setValue(RoutineStatus.ACTIVE);
        routineStartsOnPicker.setValue(null);
        draftItems.clear();
        clearItemDraftFields();
        routineSubtitleLabel.setText("Nueva rutina");
        routineStatusBadge.setText("Nueva");
        routineStatusBadge.getStyleClass().setAll("badge-ready");
        routineInfo(canManage ? "Complete cliente, título y ejercicios. Puede guardar como borrador." : "Solo lectura.");
        applyRoutineFormMode();
    }

    @FXML
    public void onAddRoutineItem() {
        if (!canManage) {
            return;
        }
        ExerciseOption exercise = itemExerciseCombo.getValue();
        if (exercise == null) {
            routineError("Seleccione un ejercicio para agregar.");
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
        draftItems.add(new RoutineItemRow(
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
        clearItemDraftFields();
        routineInfo("Ejercicio agregado. Use Detalle para ver técnica y músculos.");
    }

    @FXML
    public void onSaveRoutine() {
        if (!canManage) {
            return;
        }
        try {
            TrainingClientOption client = routineClientCombo.getValue();
            List<RoutineItemRequest> items = new ArrayList<>();
            for (RoutineItemRow row : draftItems) {
                items.add(new RoutineItemRequest(
                        row.exerciseId(), row.sets(), row.reps(), row.restSeconds(), row.loadNote(), null));
            }
            RoutineRequest request = new RoutineRequest(
                    client == null ? null : client.id(),
                    routineTitleField.getText(),
                    routineFocusField.getText(),
                    routineNotesField.getText(),
                    routineStatusCombo.getValue(),
                    routineStartsOnPicker.getValue(),
                    items);
            Long trainerId = sessionContext.requireUser().id();
            RoutineView saved = selectedRoutineId == null
                    ? trainingService.createRoutine(request, trainerId)
                    : trainingService.updateRoutine(selectedRoutineId, request, trainerId);
            routineOk(selectedRoutineId == null ? "Rutina creada." : "Rutina actualizada.");
            selectedRoutineId = saved.id();
            reloadRoutines();
            selectRoutineById(saved.id());
            loadRoutine(saved.id());
        } catch (ValidationException ex) {
            routineError(ex.getMessage());
        } catch (RuntimeException ex) {
            routineError("No se pudo guardar: " + ex.getMessage());
        }
    }

    @FXML
    public void onArchiveRoutine() {
        if (!canManage || selectedRoutineId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(
                archiveRoutineButton,
                "Archivar rutina",
                "¿Archivar esta rutina?",
                "Pasará a Archivadas. Se puede reactivar después.")) {
            return;
        }
        try {
            RoutineView view = trainingService.archiveRoutine(selectedRoutineId);
            routineOk("Rutina archivada.");
            setRoutineScope(RoutineListScope.ARCHIVED);
            selectRoutineById(view.id());
            loadRoutine(view.id());
        } catch (ValidationException ex) {
            routineError(ex.getMessage());
        } catch (RuntimeException ex) {
            routineError(ex.getMessage());
        }
    }

    @FXML
    public void onReactivateRoutine() {
        if (!canManage || selectedRoutineId == null) {
            return;
        }
        try {
            RoutineView view = trainingService.reactivateRoutine(selectedRoutineId);
            routineOk("Rutina reactivada.");
            setRoutineScope(RoutineListScope.ACTIVE);
            selectRoutineById(view.id());
            loadRoutine(view.id());
        } catch (ValidationException ex) {
            routineError(ex.getMessage());
        } catch (RuntimeException ex) {
            routineError(ex.getMessage());
        }
    }

    @FXML
    public void onNewExercise() {
        selectedExerciseId = null;
        selectedExerciseInactive = false;
        exercisesTable.getSelectionModel().clearSelection();
        exerciseNameField.clear();
        exerciseSecondaryField.clear();
        exerciseDescriptionField.clear();
        exerciseTechniqueField.clear();
        exerciseGroupCombo.setValue(MuscleGroup.OTHER);
        exerciseEquipmentCombo.setValue(EquipmentType.OTHER);
        exerciseDifficultyCombo.setValue(ExerciseDifficulty.INTERMEDIATE);
        exerciseSubtitleLabel.setText("Nuevo ejercicio");
        exerciseStatusBadge.setText("Nuevo");
        exerciseStatusBadge.getStyleClass().setAll("badge-ready");
        exerciseInfo(canManage ? "Complete nombre, grupo, equipo y nivel." : "Solo lectura.");
        applyExerciseFormMode();
    }

    @FXML
    public void onSaveExercise() {
        if (!canManage) {
            return;
        }
        try {
            ExerciseRequest request = new ExerciseRequest(
                    exerciseNameField.getText(),
                    exerciseGroupCombo.getValue(),
                    exerciseEquipmentCombo.getValue(),
                    exerciseDifficultyCombo.getValue(),
                    exerciseSecondaryField.getText(),
                    exerciseDescriptionField.getText(),
                    exerciseTechniqueField.getText(),
                    selectedExerciseId == null || !selectedExerciseInactive);
            ExerciseSummary saved = selectedExerciseId == null
                    ? trainingService.createExercise(request)
                    : trainingService.updateExercise(selectedExerciseId, request);
            exerciseOk(selectedExerciseId == null ? "Ejercicio creado." : "Ejercicio actualizado.");
            selectedExerciseId = saved.id();
            reloadExercises();
            refreshExerciseOptions();
            selectExerciseById(saved.id());
            loadExercise(saved.id());
        } catch (ValidationException ex) {
            exerciseError(ex.getMessage());
        } catch (RuntimeException ex) {
            exerciseError("No se pudo guardar: " + ex.getMessage());
        }
    }

    @FXML
    public void onDeactivateExercise() {
        if (!canManage || selectedExerciseId == null) {
            return;
        }
        if (!ConfirmDialogs.confirm(
                deactivateExerciseButton,
                "Desactivar ejercicio",
                "¿Desactivar este ejercicio?",
                "No se podrá agregar a rutinas nuevas.")) {
            return;
        }
        try {
            ExerciseSummary saved = trainingService.deactivateExercise(selectedExerciseId);
            exerciseOk("Ejercicio desactivado.");
            setExerciseFilter(Boolean.FALSE);
            refreshExerciseOptions();
            selectExerciseById(saved.id());
            loadExercise(saved.id());
        } catch (ValidationException ex) {
            exerciseError(ex.getMessage());
        } catch (RuntimeException ex) {
            exerciseError(ex.getMessage());
        }
    }

    @FXML
    public void onReactivateExercise() {
        if (!canManage || selectedExerciseId == null) {
            return;
        }
        try {
            ExerciseSummary saved = trainingService.reactivateExercise(selectedExerciseId);
            exerciseOk("Ejercicio reactivado.");
            setExerciseFilter(Boolean.TRUE);
            refreshExerciseOptions();
            selectExerciseById(saved.id());
            loadExercise(saved.id());
        } catch (ValidationException ex) {
            exerciseError(ex.getMessage());
        } catch (RuntimeException ex) {
            exerciseError(ex.getMessage());
        }
    }

    private void setupRoutineTable() {
        routineClientColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientName()));
        routineTitleColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));
        routineFocusColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().focus() == null ? "—" : d.getValue().focus()));
        routineTrainerColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().trainerName()));
        routineItemsColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().itemCount())));
        routineStatusColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().statusLabel()));
        routineStatusColumn.setCellFactory(col -> statusBadgeCell(row ->
                row.status() == RoutineStatus.ACTIVE || row.status() == RoutineStatus.SCHEDULED));
    }

    private void setupExerciseTable() {
        exerciseNameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        exerciseGroupColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().muscleGroupLabel()));
        exerciseEquipmentColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().equipmentLabel()));
        exerciseDifficultyColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().difficultyLabel()));
        exerciseStatusColumn.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().active() ? "Activo" : "Inactivo"));
        exerciseStatusColumn.setCellFactory(col -> statusBadgeCell(ExerciseSummary::active));
    }

    private <T> TableCell<T, String> statusBadgeCell(java.util.function.Predicate<T> activePred) {
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
                badge.getStyleClass().setAll(
                        "table-status-badge",
                        activePred.test(row) ? "badge-paid" : "badge-cancelled");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        };
    }

    private void setupCombos() {
        exerciseGroupCombo.setItems(FXCollections.observableArrayList(MuscleGroup.values()));
        exerciseGroupCombo.setConverter(labelConverter(TrainingService::labelForMuscle));
        exerciseEquipmentCombo.setItems(FXCollections.observableArrayList(EquipmentType.values()));
        exerciseEquipmentCombo.setConverter(labelConverter(TrainingService::labelForEquipment));
        exerciseDifficultyCombo.setItems(FXCollections.observableArrayList(ExerciseDifficulty.values()));
        exerciseDifficultyCombo.setConverter(labelConverter(TrainingService::labelForDifficulty));
        routineStatusCombo.setItems(FXCollections.observableArrayList(
                RoutineStatus.ACTIVE, RoutineStatus.DRAFT, RoutineStatus.SCHEDULED, RoutineStatus.ARCHIVED));
        routineStatusCombo.setConverter(labelConverter(TrainingService::labelForRoutineStatus));

        routineClientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(TrainingClientOption option) {
                return option == null ? "" : option.fullName() + " · " + option.documentNumber();
            }

            @Override
            public TrainingClientOption fromString(String string) {
                return null;
            }
        });
        routineClientCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TrainingClientOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.fullName() + " · " + item.documentNumber());
            }
        });

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

    private <T> StringConverter<T> labelConverter(java.util.function.Function<T, String> labels) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : labels.apply(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    private void setupItemsTable() {
        routineItemsTable.setItems(draftItems);
        itemNameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().exerciseName()));
        itemSetsColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().sets() == null ? "—" : String.valueOf(d.getValue().sets())));
        itemRepsColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().reps() == null ? "—" : d.getValue().reps()));
        itemRestColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().restSeconds() == null ? "—" : d.getValue().restSeconds() + "s"));
        itemLoadColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().loadNote() == null ? "—" : d.getValue().loadNote()));

        itemDetailColumn.setCellFactory(col -> new TableCell<>() {
            private final Button detail = new Button("Ver");

            {
                detail.getStyleClass().add("secondary-button");
                detail.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < draftItems.size()) {
                        showItemDetail(draftItems.get(getIndex()));
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getIndex() < 0 || getIndex() >= draftItems.size() ? null : detail);
            }
        });

        itemRemoveColumn.setCellFactory(col -> new TableCell<>() {
            private final Button remove = new Button("Quitar");

            {
                remove.getStyleClass().add("danger-button");
                remove.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < draftItems.size()) {
                        draftItems.remove(getIndex());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= draftItems.size()) {
                    setGraphic(null);
                    return;
                }
                remove.setDisable(!canManage || selectedRoutineArchived);
                setGraphic(remove);
            }
        });
    }

    private void showItemDetail(RoutineItemRow row) {
        Window owner = routineItemsTable.getScene() == null ? null : routineItemsTable.getScene().getWindow();
        RoutineItemDetailController.open(owner, row);
    }

    private void reloadRoutines() {
        List<RoutineSummary> rows = trainingService.listRoutines(routineSearchField.getText(), routineScope);
        routinesTable.setItems(FXCollections.observableArrayList(rows));
        routinesCountLabel.setText(rows.size() + (rows.size() == 1 ? " rutina" : " rutinas"));
    }

    private void reloadExercises() {
        List<ExerciseSummary> rows = trainingService.listExercises(exerciseSearchField.getText(), exerciseActiveOnly);
        exercisesTable.setItems(FXCollections.observableArrayList(rows));
        exercisesCountLabel.setText(rows.size() + (rows.size() == 1 ? " ejercicio" : " ejercicios"));
    }

    private void refreshClientOptions() {
        routineClientCombo.setItems(FXCollections.observableArrayList(trainingService.listActiveClients()));
    }

    private void refreshExerciseOptions() {
        itemExerciseCombo.setItems(FXCollections.observableArrayList(trainingService.listActiveExerciseOptions()));
    }

    private void loadRoutine(Long id) {
        try {
            RoutineView view = trainingService.getRoutine(id);
            selectedRoutineId = view.id();
            selectedRoutineArchived = view.status() == RoutineStatus.ARCHIVED;
            routineClientCombo.getItems().stream()
                    .filter(c -> c.id().equals(view.clientId()))
                    .findFirst()
                    .ifPresentOrElse(routineClientCombo::setValue, () ->
                            routineClientCombo.setValue(new TrainingClientOption(
                                    view.clientId(), view.clientDocument(), view.clientName(), null)));
            routineTitleField.setText(view.title());
            routineFocusField.setText(view.focus() == null ? "" : view.focus());
            routineNotesField.setText(view.notes() == null ? "" : view.notes());
            routineStatusCombo.setValue(view.status());
            routineStartsOnPicker.setValue(view.startsOn());
            draftItems.setAll(view.items().stream()
                    .map(i -> new RoutineItemRow(
                            i.exerciseId(),
                            i.exerciseName(),
                            i.muscleGroupLabel(),
                            i.equipmentLabel(),
                            i.difficultyLabel(),
                            i.secondaryMuscles(),
                            i.description(),
                            i.techniqueNotes(),
                            i.sets(),
                            i.reps(),
                            i.restSeconds(),
                            i.loadNote()))
                    .toList());
            routineSubtitleLabel.setText(view.clientName() + " · " + view.trainerName());
            routineStatusBadge.setText(view.statusLabel());
            routineStatusBadge.getStyleClass().setAll(
                    view.status() == RoutineStatus.ARCHIVED ? "badge-cancelled" : "badge-paid");
            routineInfo(selectedRoutineArchived
                    ? "Rutina archivada. Puede reactivarla."
                    : "Use Ver en cada ejercicio para ver técnica y músculos.");
            applyRoutineFormMode();
        } catch (RuntimeException ex) {
            routineError(ex.getMessage());
        }
    }

    private void loadExercise(Long id) {
        try {
            ExerciseSummary view = trainingService.getExercise(id);
            selectedExerciseId = view.id();
            selectedExerciseInactive = !view.active();
            exerciseNameField.setText(view.name());
            exerciseGroupCombo.setValue(view.muscleGroup());
            exerciseEquipmentCombo.setValue(view.equipment());
            exerciseDifficultyCombo.setValue(view.difficulty());
            exerciseSecondaryField.setText(view.secondaryMuscles() == null ? "" : view.secondaryMuscles());
            exerciseDescriptionField.setText(view.description() == null ? "" : view.description());
            exerciseTechniqueField.setText(view.techniqueNotes() == null ? "" : view.techniqueNotes());
            exerciseSubtitleLabel.setText(view.muscleGroupLabel() + " · " + view.equipmentLabel());
            exerciseStatusBadge.setText(view.active() ? "Activo" : "Inactivo");
            exerciseStatusBadge.getStyleClass().setAll(view.active() ? "badge-paid" : "badge-cancelled");
            exerciseInfo(view.active()
                    ? "Puede editar ficha técnica del ejercicio."
                    : "Ejercicio inactivo. Puede reactivarlo.");
            applyExerciseFormMode();
        } catch (RuntimeException ex) {
            exerciseError(ex.getMessage());
        }
    }

    private void setRoutineScope(RoutineListScope next) {
        routineScope = next;
        applyChip(filterActiveRoutinesButton, next == RoutineListScope.ACTIVE);
        applyChip(filterDraftRoutinesButton, next == RoutineListScope.DRAFT);
        applyChip(filterScheduledRoutinesButton, next == RoutineListScope.SCHEDULED);
        applyChip(filterArchivedRoutinesButton, next == RoutineListScope.ARCHIVED);
        applyChip(filterAllRoutinesButton, next == RoutineListScope.ALL);
        reloadRoutines();
    }

    private void setExerciseFilter(Boolean activeOnly) {
        exerciseActiveOnly = activeOnly;
        applyChip(filterActiveExercisesButton, Boolean.TRUE.equals(activeOnly));
        applyChip(filterInactiveExercisesButton, Boolean.FALSE.equals(activeOnly));
        applyChip(filterAllExercisesButton, activeOnly == null);
        reloadExercises();
    }

    private void applyRoutineFormMode() {
        boolean creating = selectedRoutineId == null;
        boolean editable = canManage && (creating || !selectedRoutineArchived);
        routineClientCombo.setDisable(!canManage || !creating);
        routineTitleField.setEditable(editable);
        routineFocusField.setEditable(editable);
        routineNotesField.setEditable(editable);
        routineStatusCombo.setDisable(!editable);
        routineStartsOnPicker.setDisable(!editable || routineStatusCombo.getValue() != RoutineStatus.SCHEDULED);
        addItemButton.setDisable(!editable);
        itemExerciseCombo.setDisable(!editable);
        itemSetsField.setDisable(!editable);
        itemRepsField.setDisable(!editable);
        itemRestField.setDisable(!editable);
        itemLoadField.setDisable(!editable);
        saveRoutineButton.setDisable(!editable);
        archiveRoutineButton.setDisable(!canManage || creating || selectedRoutineArchived);
        reactivateRoutineButton.setDisable(!canManage || creating || !selectedRoutineArchived);
        reactivateRoutineButton.setVisible(!creating && selectedRoutineArchived);
        reactivateRoutineButton.setManaged(!creating && selectedRoutineArchived);
        routineItemsTable.refresh();
    }

    private void applyExerciseFormMode() {
        boolean creating = selectedExerciseId == null;
        boolean editable = canManage && (creating || !selectedExerciseInactive);
        exerciseNameField.setEditable(editable);
        exerciseSecondaryField.setEditable(editable);
        exerciseDescriptionField.setEditable(editable);
        exerciseTechniqueField.setEditable(editable);
        exerciseGroupCombo.setDisable(!editable);
        exerciseEquipmentCombo.setDisable(!editable);
        exerciseDifficultyCombo.setDisable(!editable);
        saveExerciseButton.setDisable(!canManage || (!creating && selectedExerciseInactive));
        deactivateExerciseButton.setDisable(!canManage || creating || selectedExerciseInactive);
        reactivateExerciseButton.setDisable(!canManage || creating || !selectedExerciseInactive);
        reactivateExerciseButton.setVisible(!creating && selectedExerciseInactive);
        reactivateExerciseButton.setManaged(!creating && selectedExerciseInactive);
    }

    private void applyPermissions() {
        newRoutineButton.setDisable(!canManage);
        saveRoutineButton.setDisable(!canManage);
        archiveRoutineButton.setDisable(!canManage);
        reactivateRoutineButton.setDisable(!canManage);
        addItemButton.setDisable(!canManage);
        newExerciseButton.setDisable(!canManage);
        saveExerciseButton.setDisable(!canManage);
        deactivateExerciseButton.setDisable(!canManage);
        reactivateExerciseButton.setDisable(!canManage);
        if (!canManage) {
            routineInfo("Solo lectura: su rol no administra entrenamiento.");
            exerciseInfo("Solo lectura.");
        }
    }

    private void selectRoutineById(Long id) {
        routinesTable.getItems().stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .ifPresent(r -> routinesTable.getSelectionModel().select(r));
    }

    private void selectExerciseById(Long id) {
        exercisesTable.getItems().stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .ifPresent(e -> exercisesTable.getSelectionModel().select(e));
    }

    private Integer parsePositiveInt(String raw, String label) {
        String value = blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                routineError("Las " + label + " deben ser mayores a 0.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            routineError("Valor inválido en " + label + ".");
            return null;
        }
    }

    private Integer parseNonNegativeInt(String raw, String label) {
        String value = blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                routineError("El " + label + " no puede ser negativo.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            routineError("Valor inválido en " + label + ".");
            return null;
        }
    }

    private void clearItemDraftFields() {
        itemExerciseCombo.setValue(null);
        itemSetsField.clear();
        itemRepsField.clear();
        itemRestField.clear();
        itemLoadField.clear();
    }

    private static void applyChip(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private void routineInfo(String message) {
        routineStatusLabel.getStyleClass().setAll("muted");
        routineStatusLabel.setText(message == null ? "" : message);
    }

    private void routineOk(String message) {
        routineStatusLabel.getStyleClass().setAll("status-ok");
        routineStatusLabel.setText(message);
    }

    private void routineError(String message) {
        routineStatusLabel.getStyleClass().setAll("status-error");
        routineStatusLabel.setText(message);
    }

    private void exerciseInfo(String message) {
        exerciseStatusLabel.getStyleClass().setAll("muted");
        exerciseStatusLabel.setText(message == null ? "" : message);
    }

    private void exerciseOk(String message) {
        exerciseStatusLabel.getStyleClass().setAll("status-ok");
        exerciseStatusLabel.setText(message);
    }

    private void exerciseError(String message) {
        exerciseStatusLabel.getStyleClass().setAll("status-error");
        exerciseStatusLabel.setText(message);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
