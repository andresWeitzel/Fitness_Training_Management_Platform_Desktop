package com.fitnesstraining.training.service;

import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.training.dto.ExerciseRequest;
import com.fitnesstraining.training.dto.RoutineItemRequest;
import com.fitnesstraining.training.dto.RoutineRequest;
import com.fitnesstraining.training.model.EquipmentType;
import com.fitnesstraining.training.model.Exercise;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;
import com.fitnesstraining.training.model.RoutineStatus;
import com.fitnesstraining.training.model.TrainingRoutine;
import com.fitnesstraining.training.repository.ExerciseRepository;
import com.fitnesstraining.training.repository.TrainingRoutineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-24T15:00:00Z"), ZoneOffset.UTC);

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private TrainingRoutineRepository routineRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AccessCredentialRepository credentialRepository;
    @Mock private UserRepository userRepository;

    private TrainingService trainingService;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService(
                exerciseRepository,
                routineRepository,
                clientRepository,
                credentialRepository,
                userRepository,
                CLOCK);
    }

    @Test
    void rejectsDuplicateExerciseName() {
        when(exerciseRepository.existsName("Press banca", null)).thenReturn(true);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                trainingService.createExercise(new ExerciseRequest(
                        "Press banca",
                        MuscleGroup.CHEST,
                        EquipmentType.BARBELL,
                        ExerciseDifficulty.INTERMEDIATE,
                        null,
                        null,
                        null,
                        true)));
        assertEquals("Ya existe un ejercicio con ese nombre.", ex.getMessage());
    }

    @Test
    void rejectsRoutineWithoutItemsUnlessDraft() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                trainingService.createRoutine(new RoutineRequest(
                        1L, "Rutina", null, null, RoutineStatus.ACTIVE, null, List.of()), 10L));
        assertEquals("Agregue al menos un ejercicio a la rutina.", ex.getMessage());
    }

    @Test
    void createsRoutineWithActiveClientAndExercise() {
        Client client = withId(Client.register(
                "30111222", "Ana", "Lopez", null, null, null, OffsetDateTime.now(CLOCK)), 5L);
        Exercise exercise = withId(Exercise.create(
                "Sentadilla",
                MuscleGroup.LEGS,
                EquipmentType.BARBELL,
                ExerciseDifficulty.INTERMEDIATE,
                null,
                null,
                null,
                OffsetDateTime.now(CLOCK)), 3L);
        when(clientRepository.findActiveById(5L)).thenReturn(Optional.of(client));
        when(exerciseRepository.findById(3L)).thenReturn(Optional.of(exercise));
        when(routineRepository.save(any(TrainingRoutine.class))).thenAnswer(inv -> {
            TrainingRoutine routine = inv.getArgument(0);
            withId(routine, ids.getAndIncrement());
            return routine;
        });
        when(routineRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            TrainingRoutine routine = TrainingRoutine.create(
                    5L, 10L, "Full body", "Hipertrofia", null,
                    RoutineStatus.ACTIVE, null, OffsetDateTime.now(CLOCK));
            withId(routine, id);
            routine.replaceItems(List.of(
                    com.fitnesstraining.training.model.TrainingRoutineItem.of(3L, 4, "10", 90, null, null)
            ), OffsetDateTime.now(CLOCK));
            return Optional.of(routine);
        });
        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        var view = trainingService.createRoutine(new RoutineRequest(
                5L,
                "Full body",
                "Hipertrofia",
                null,
                RoutineStatus.ACTIVE,
                null,
                List.of(new RoutineItemRequest(3L, 4, "10", 90, null, null))
        ), 10L);

        assertEquals("Full body", view.title());
        assertEquals("Hipertrofia", view.focus());
        assertEquals(5L, view.clientId());
        assertEquals(1, view.items().size());
        assertEquals("Sentadilla", view.items().get(0).exerciseName());
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
