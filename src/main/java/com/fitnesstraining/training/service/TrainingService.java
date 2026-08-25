package com.fitnesstraining.training.service;

import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientListScope;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.shared.exception.AppException;
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
import com.fitnesstraining.training.model.Exercise;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;
import com.fitnesstraining.training.model.RoutineListScope;
import com.fitnesstraining.training.model.RoutineStatus;
import com.fitnesstraining.training.model.TrainingRoutine;
import com.fitnesstraining.training.model.TrainingRoutineItem;
import com.fitnesstraining.training.repository.ExerciseRepository;
import com.fitnesstraining.training.repository.TrainingRoutineRepository;
import com.fitnesstraining.training.validation.TrainingValidator;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TrainingService {

    private final ExerciseRepository exerciseRepository;
    private final TrainingRoutineRepository routineRepository;
    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public TrainingService(
            ExerciseRepository exerciseRepository,
            TrainingRoutineRepository routineRepository,
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            UserRepository userRepository,
            Clock clock) {
        this.exerciseRepository = exerciseRepository;
        this.routineRepository = routineRepository;
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public List<ExerciseSummary> listExercises(String term, Boolean activeOnly) {
        List<Exercise> exercises;
        if (term == null || term.isBlank()) {
            if (activeOnly == null) {
                exercises = exerciseRepository.findAll();
            } else if (activeOnly) {
                exercises = exerciseRepository.findActive();
            } else {
                exercises = exerciseRepository.findAll().stream().filter(e -> !e.isActive()).toList();
            }
        } else {
            exercises = exerciseRepository.search(term.trim(), activeOnly);
        }
        return exercises.stream().map(this::toExerciseSummary).toList();
    }

    public List<ExerciseOption> listActiveExerciseOptions() {
        return exerciseRepository.findActive().stream()
                .map(e -> new ExerciseOption(
                        e.getId(),
                        e.getName(),
                        e.getMuscleGroup(),
                        e.getEquipment(),
                        e.getDifficulty(),
                        e.getSecondaryMuscles(),
                        e.getDescription(),
                        e.getTechniqueNotes(),
                        labelForMuscle(e.getMuscleGroup()) + " · " + e.getName()
                                + " · " + labelForEquipment(e.getEquipment())))
                .toList();
    }

    public ExerciseSummary getExercise(Long id) {
        return toExerciseSummary(requireExercise(id));
    }

    public ExerciseSummary createExercise(ExerciseRequest request) {
        ExerciseRequest normalized = TrainingValidator.normalizeExercise(request);
        if (exerciseRepository.existsName(normalized.name(), null)) {
            throw new ValidationException("Ya existe un ejercicio con ese nombre.");
        }
        Exercise exercise = Exercise.create(
                normalized.name(),
                normalized.muscleGroup(),
                normalized.equipment(),
                normalized.difficulty(),
                normalized.secondaryMuscles(),
                normalized.description(),
                normalized.techniqueNotes(),
                now());
        return toExerciseSummary(exerciseRepository.save(exercise));
    }

    public ExerciseSummary updateExercise(Long id, ExerciseRequest request) {
        Exercise exercise = requireExercise(id);
        ExerciseRequest normalized = TrainingValidator.normalizeExercise(request);
        if (exerciseRepository.existsName(normalized.name(), id)) {
            throw new ValidationException("Ya existe un ejercicio con ese nombre.");
        }
        exercise.update(
                normalized.name(),
                normalized.muscleGroup(),
                normalized.equipment(),
                normalized.difficulty(),
                normalized.secondaryMuscles(),
                normalized.description(),
                normalized.techniqueNotes(),
                normalized.active() == null || normalized.active(),
                now());
        return toExerciseSummary(exerciseRepository.save(exercise));
    }

    public ExerciseSummary deactivateExercise(Long id) {
        Exercise exercise = requireExercise(id);
        if (!exercise.isActive()) {
            throw new ValidationException("El ejercicio ya está inactivo.");
        }
        exercise.deactivate(now());
        return toExerciseSummary(exerciseRepository.save(exercise));
    }

    public ExerciseSummary reactivateExercise(Long id) {
        Exercise exercise = requireExercise(id);
        if (exercise.isActive()) {
            throw new ValidationException("El ejercicio ya está activo.");
        }
        exercise.reactivate(now());
        return toExerciseSummary(exerciseRepository.save(exercise));
    }

    public List<TrainingClientOption> listActiveClients() {
        return clientRepository.list(ClientListScope.ACTIVE).stream()
                .map(c -> new TrainingClientOption(
                        c.getId(),
                        c.getDocumentNumber(),
                        c.fullName(),
                        credentialRepository.findClientNumber(c.getId()).orElse(null)))
                .toList();
    }

    public List<RoutineSummary> listRoutines(String term, RoutineListScope scope) {
        RoutineStatus status = switch (scope) {
            case ACTIVE -> RoutineStatus.ACTIVE;
            case DRAFT -> RoutineStatus.DRAFT;
            case SCHEDULED -> RoutineStatus.SCHEDULED;
            case ARCHIVED -> RoutineStatus.ARCHIVED;
            case ALL -> null;
        };
        List<TrainingRoutine> routines = (term == null || term.isBlank())
                ? routineRepository.list(status)
                : routineRepository.search(term.trim(), status);

        Set<Long> clientIds = routines.stream().map(TrainingRoutine::getClientId).collect(Collectors.toSet());
        Set<Long> trainerIds = routines.stream().map(TrainingRoutine::getTrainerUserId).collect(Collectors.toSet());
        Map<Long, Client> clients = loadClients(clientIds);
        Map<Long, User> trainers = loadUsers(trainerIds);

        String needle = term == null ? "" : term.trim().toLowerCase();
        return routines.stream()
                .map(r -> toRoutineSummary(r, clients.get(r.getClientId()), trainers.get(r.getTrainerUserId())))
                .filter(summary -> needle.isBlank()
                        || summary.title().toLowerCase().contains(needle)
                        || (summary.focus() != null && summary.focus().toLowerCase().contains(needle))
                        || (summary.clientName() != null && summary.clientName().toLowerCase().contains(needle))
                        || (summary.clientDocument() != null && summary.clientDocument().toLowerCase().contains(needle))
                        || (summary.trainerName() != null && summary.trainerName().toLowerCase().contains(needle)))
                .toList();
    }

    public RoutineView getRoutine(Long id) {
        TrainingRoutine routine = requireRoutine(id);
        Client client = requireActiveOrAnyClient(routine.getClientId());
        User trainer = userRepository.findById(routine.getTrainerUserId()).orElse(null);
        Map<Long, Exercise> exercises = loadExercises(routine.getItems().stream()
                .map(TrainingRoutineItem::getExerciseId)
                .collect(Collectors.toSet()));
        return toRoutineView(routine, client, trainer, exercises);
    }

    public RoutineView createRoutine(RoutineRequest request, Long trainerUserId) {
        if (trainerUserId == null) {
            throw new ValidationException("No hay entrenador en sesión.");
        }
        RoutineRequest normalized = TrainingValidator.normalizeRoutine(request);
        Client client = clientRepository.findActiveById(normalized.clientId())
                .orElseThrow(() -> new ValidationException("Seleccione un cliente activo."));
        List<TrainingRoutineItem> items = buildItems(normalized.items());
        TrainingRoutine routine = TrainingRoutine.create(
                client.getId(),
                trainerUserId,
                normalized.title(),
                normalized.focus(),
                normalized.notes(),
                normalized.status(),
                normalized.startsOn(),
                now());
        routine.replaceItems(items, now());
        TrainingRoutine saved = routineRepository.save(routine);
        return getRoutine(saved.getId());
    }

    public RoutineView updateRoutine(Long id, RoutineRequest request, Long trainerUserId) {
        TrainingRoutine routine = requireRoutine(id);
        if (routine.getStatus() == RoutineStatus.ARCHIVED) {
            throw new ValidationException("Reactive la rutina antes de editarla.");
        }
        RoutineRequest normalized = TrainingValidator.normalizeRoutine(request);
        if (!normalized.clientId().equals(routine.getClientId())) {
            throw new ValidationException("No se puede cambiar el cliente de una rutina existente.");
        }
        List<TrainingRoutineItem> items = buildItems(normalized.items());
        routine.update(
                normalized.title(),
                normalized.focus(),
                normalized.notes(),
                normalized.status(),
                normalized.startsOn(),
                trainerUserId,
                now());
        routine.replaceItems(items, now());
        routineRepository.save(routine);
        return getRoutine(id);
    }

    public RoutineView archiveRoutine(Long id) {
        TrainingRoutine routine = requireRoutine(id);
        if (routine.getStatus() == RoutineStatus.ARCHIVED) {
            throw new ValidationException("La rutina ya está archivada.");
        }
        routine.changeStatus(RoutineStatus.ARCHIVED, now());
        routineRepository.save(routine);
        return getRoutine(id);
    }

    public RoutineView reactivateRoutine(Long id) {
        TrainingRoutine routine = requireRoutine(id);
        if (routine.getStatus() == RoutineStatus.ACTIVE) {
            throw new ValidationException("La rutina ya está activa.");
        }
        clientRepository.findActiveById(routine.getClientId())
                .orElseThrow(() -> new ValidationException(
                        "El cliente de esta rutina está dado de baja. Reactive el cliente primero."));
        routine.changeStatus(RoutineStatus.ACTIVE, now());
        routineRepository.save(routine);
        return getRoutine(id);
    }

    public static String labelForMuscle(MuscleGroup group) {
        if (group == null) {
            return "Otro";
        }
        return switch (group) {
            case CHEST -> "Pecho";
            case BACK -> "Espalda";
            case LEGS -> "Piernas";
            case SHOULDERS -> "Hombros";
            case ARMS -> "Brazos";
            case CORE -> "Core";
            case CARDIO -> "Cardio";
            case OTHER -> "Otro";
        };
    }

    public static String labelForEquipment(EquipmentType equipment) {
        if (equipment == null) {
            return "Otro";
        }
        return switch (equipment) {
            case BARBELL -> "Barra";
            case DUMBBELL -> "Mancuernas";
            case MACHINE -> "Máquina";
            case CABLE -> "Polea / cable";
            case BODYWEIGHT -> "Peso corporal";
            case BAND -> "Banda";
            case KETTLEBELL -> "Kettlebell";
            case CARDIO_MACHINE -> "Cardio";
            case OTHER -> "Otro";
        };
    }

    public static String labelForDifficulty(ExerciseDifficulty difficulty) {
        if (difficulty == null) {
            return "Intermedio";
        }
        return switch (difficulty) {
            case BEGINNER -> "Principiante";
            case INTERMEDIATE -> "Intermedio";
            case ADVANCED -> "Avanzado";
        };
    }

    public static String labelForRoutineStatus(RoutineStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case ACTIVE -> "Activa";
            case DRAFT -> "Borrador";
            case SCHEDULED -> "Programada";
            case ARCHIVED -> "Archivada";
        };
    }

    private List<TrainingRoutineItem> buildItems(List<RoutineItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        Set<Long> exerciseIds = requests.stream().map(RoutineItemRequest::exerciseId).collect(Collectors.toSet());
        Map<Long, Exercise> exercises = loadExercises(exerciseIds);
        List<TrainingRoutineItem> items = new ArrayList<>();
        for (RoutineItemRequest request : requests) {
            Exercise exercise = exercises.get(request.exerciseId());
            if (exercise == null) {
                throw new ValidationException("Ejercicio no encontrado.");
            }
            if (!exercise.isActive()) {
                throw new ValidationException("El ejercicio \"" + exercise.getName() + "\" está inactivo.");
            }
            items.add(TrainingRoutineItem.of(
                    exercise.getId(),
                    request.sets(),
                    request.reps(),
                    request.restSeconds(),
                    request.loadNote(),
                    request.notes()));
        }
        return items;
    }

    private Exercise requireExercise(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new AppException("Ejercicio no encontrado."));
    }

    private TrainingRoutine requireRoutine(Long id) {
        return routineRepository.findById(id)
                .orElseThrow(() -> new AppException("Rutina no encontrada."));
    }

    private Client requireActiveOrAnyClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new AppException("Cliente no encontrado."));
    }

    private Map<Long, Client> loadClients(Set<Long> ids) {
        Map<Long, Client> map = new HashMap<>();
        for (Long id : ids) {
            clientRepository.findById(id).ifPresent(c -> map.put(id, c));
        }
        return map;
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        Map<Long, User> map = new HashMap<>();
        for (Long id : ids) {
            userRepository.findById(id).ifPresent(u -> map.put(id, u));
        }
        return map;
    }

    private Map<Long, Exercise> loadExercises(Set<Long> ids) {
        Map<Long, Exercise> map = new HashMap<>();
        for (Long id : ids) {
            exerciseRepository.findById(id).ifPresent(e -> map.put(id, e));
        }
        return map;
    }

    private ExerciseSummary toExerciseSummary(Exercise exercise) {
        return new ExerciseSummary(
                exercise.getId(),
                exercise.getName(),
                exercise.getMuscleGroup(),
                labelForMuscle(exercise.getMuscleGroup()),
                exercise.getEquipment(),
                labelForEquipment(exercise.getEquipment()),
                exercise.getDifficulty(),
                labelForDifficulty(exercise.getDifficulty()),
                exercise.getSecondaryMuscles(),
                exercise.getDescription(),
                exercise.getTechniqueNotes(),
                exercise.isActive());
    }

    private RoutineSummary toRoutineSummary(TrainingRoutine routine, Client client, User trainer) {
        return new RoutineSummary(
                routine.getId(),
                routine.getClientId(),
                client == null ? "Cliente #" + routine.getClientId() : client.fullName(),
                client == null ? "—" : client.getDocumentNumber(),
                routine.getTitle(),
                routine.getFocus(),
                routine.getItems() == null ? 0 : routine.getItems().size(),
                routine.getStatus(),
                labelForRoutineStatus(routine.getStatus()),
                trainer == null ? "—" : trainer.getDisplayName(),
                routine.getStartsOn(),
                routine.getUpdatedAt());
    }

    private RoutineView toRoutineView(
            TrainingRoutine routine,
            Client client,
            User trainer,
            Map<Long, Exercise> exercises) {
        List<RoutineView.RoutineItemView> items = routine.getItems().stream()
                .map(item -> {
                    Exercise exercise = exercises.get(item.getExerciseId());
                    return new RoutineView.RoutineItemView(
                            item.getId(),
                            item.getExerciseId(),
                            exercise == null ? "Ejercicio #" + item.getExerciseId() : exercise.getName(),
                            exercise == null ? null : exercise.getMuscleGroup(),
                            labelForMuscle(exercise == null ? null : exercise.getMuscleGroup()),
                            exercise == null ? null : exercise.getEquipment(),
                            labelForEquipment(exercise == null ? null : exercise.getEquipment()),
                            exercise == null ? null : exercise.getDifficulty(),
                            labelForDifficulty(exercise == null ? null : exercise.getDifficulty()),
                            exercise == null ? null : exercise.getSecondaryMuscles(),
                            exercise == null ? null : exercise.getDescription(),
                            exercise == null ? null : exercise.getTechniqueNotes(),
                            item.getSets(),
                            item.getReps(),
                            item.getRestSeconds(),
                            item.getLoadNote(),
                            item.getNotes(),
                            item.getSortOrder());
                })
                .toList();
        return new RoutineView(
                routine.getId(),
                routine.getClientId(),
                client.fullName(),
                client.getDocumentNumber(),
                routine.getTrainerUserId(),
                trainer == null ? "—" : trainer.getDisplayName(),
                routine.getTitle(),
                routine.getFocus(),
                routine.getNotes(),
                routine.getStatus(),
                labelForRoutineStatus(routine.getStatus()),
                routine.getStartsOn(),
                routine.getCreatedAt(),
                routine.getUpdatedAt(),
                items);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
