package com.fitnesstraining.training.validation;

import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.training.dto.ExerciseRequest;
import com.fitnesstraining.training.dto.RoutineItemRequest;
import com.fitnesstraining.training.dto.RoutineRequest;
import com.fitnesstraining.training.model.RoutineStatus;

import java.util.ArrayList;
import java.util.List;

public final class TrainingValidator {

    private TrainingValidator() {
    }

    public static ExerciseRequest normalizeExercise(ExerciseRequest request) {
        if (request == null) {
            throw new ValidationException("Complete los datos del ejercicio.");
        }
        String name = blankToNull(request.name());
        String description = blankToNull(request.description());
        String secondary = blankToNull(request.secondaryMuscles());
        String technique = blankToNull(request.techniqueNotes());
        if (name == null) {
            throw new ValidationException("El nombre del ejercicio es obligatorio.");
        }
        if (name.length() > 120) {
            throw new ValidationException("El nombre no puede superar 120 caracteres.");
        }
        if (request.muscleGroup() == null) {
            throw new ValidationException("Seleccione el grupo muscular.");
        }
        if (request.equipment() == null) {
            throw new ValidationException("Seleccione el equipamiento.");
        }
        if (request.difficulty() == null) {
            throw new ValidationException("Seleccione el nivel de dificultad.");
        }
        if (secondary != null && secondary.length() > 200) {
            throw new ValidationException("Los músculos secundarios no pueden superar 200 caracteres.");
        }
        if (description != null && description.length() > 500) {
            throw new ValidationException("La descripción no puede superar 500 caracteres.");
        }
        if (technique != null && technique.length() > 1000) {
            throw new ValidationException("Las notas técnicas no pueden superar 1000 caracteres.");
        }
        boolean active = request.active() == null || request.active();
        return new ExerciseRequest(
                name,
                request.muscleGroup(),
                request.equipment(),
                request.difficulty(),
                secondary,
                description,
                technique,
                active);
    }

    public static RoutineRequest normalizeRoutine(RoutineRequest request) {
        if (request == null) {
            throw new ValidationException("Complete los datos de la rutina.");
        }
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        String title = blankToNull(request.title());
        String focus = blankToNull(request.focus());
        String notes = blankToNull(request.notes());
        if (title == null) {
            throw new ValidationException("El título de la rutina es obligatorio.");
        }
        if (title.length() > 150) {
            throw new ValidationException("El título no puede superar 150 caracteres.");
        }
        if (focus != null && focus.length() > 80) {
            throw new ValidationException("El enfoque no puede superar 80 caracteres.");
        }
        if (notes != null && notes.length() > 1000) {
            throw new ValidationException("Las observaciones no pueden superar 1000 caracteres.");
        }
        RoutineStatus status = request.status() == null ? RoutineStatus.ACTIVE : request.status();
        if (status == RoutineStatus.SCHEDULED && request.startsOn() == null) {
            throw new ValidationException("Indique la fecha de inicio para una rutina programada.");
        }
        List<RoutineItemRequest> items = request.items() == null ? List.of() : request.items();
        if (items.isEmpty() && status != RoutineStatus.DRAFT) {
            throw new ValidationException("Agregue al menos un ejercicio a la rutina.");
        }
        List<RoutineItemRequest> normalizedItems = new ArrayList<>();
        for (RoutineItemRequest item : items) {
            normalizedItems.add(normalizeItem(item));
        }
        return new RoutineRequest(
                request.clientId(),
                title,
                focus,
                notes,
                status,
                request.startsOn(),
                List.copyOf(normalizedItems));
    }

    private static RoutineItemRequest normalizeItem(RoutineItemRequest item) {
        if (item == null || item.exerciseId() == null) {
            throw new ValidationException("Cada ítem debe tener un ejercicio.");
        }
        Integer sets = item.sets();
        if (sets != null && sets <= 0) {
            throw new ValidationException("Las series deben ser mayores a 0.");
        }
        Integer rest = item.restSeconds();
        if (rest != null && rest < 0) {
            throw new ValidationException("El descanso no puede ser negativo.");
        }
        String reps = blankToNull(item.reps());
        String loadNote = blankToNull(item.loadNote());
        String notes = blankToNull(item.notes());
        if (reps != null && reps.length() > 40) {
            throw new ValidationException("Las repeticiones no pueden superar 40 caracteres.");
        }
        if (loadNote != null && loadNote.length() > 80) {
            throw new ValidationException("La carga no puede superar 80 caracteres.");
        }
        if (notes != null && notes.length() > 300) {
            throw new ValidationException("Las notas del ítem no pueden superar 300 caracteres.");
        }
        return new RoutineItemRequest(item.exerciseId(), sets, reps, rest, loadNote, notes);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
