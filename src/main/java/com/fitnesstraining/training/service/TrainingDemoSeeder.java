package com.fitnesstraining.training.service;

import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientListScope;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.training.dto.RoutineItemRequest;
import com.fitnesstraining.training.dto.RoutineRequest;
import com.fitnesstraining.training.model.EquipmentType;
import com.fitnesstraining.training.model.Exercise;
import com.fitnesstraining.training.model.ExerciseDifficulty;
import com.fitnesstraining.training.model.MuscleGroup;
import com.fitnesstraining.training.model.RoutineStatus;
import com.fitnesstraining.training.repository.ExerciseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class TrainingDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(TrainingDemoSeeder.class);

    private final ExerciseRepository exerciseRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TrainingService trainingService;
    private final Clock clock;

    public TrainingDemoSeeder(
            ExerciseRepository exerciseRepository,
            ClientRepository clientRepository,
            UserRepository userRepository,
            TrainingService trainingService,
            Clock clock) {
        this.exerciseRepository = exerciseRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.trainingService = trainingService;
        this.clock = clock;
    }

    public void seedIfEmpty() {
        if (exerciseRepository.count() > 0) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Exercise press = save(Exercise.create(
                "Press banca", MuscleGroup.CHEST, EquipmentType.BARBELL, ExerciseDifficulty.INTERMEDIATE,
                "Tríceps, deltoides anterior", "Barra en banco plano",
                "Escápulas retraídas. Baja controlado hasta el pecho.", now));
        Exercise sentadilla = save(Exercise.create(
                "Sentadilla", MuscleGroup.LEGS, EquipmentType.BARBELL, ExerciseDifficulty.INTERMEDIATE,
                "Glúteos, core", "Sentadilla libre",
                "Rodillas alineadas con pies. Profundidad cómoda.", now));
        Exercise pesoMuerto = save(Exercise.create(
                "Peso muerto", MuscleGroup.BACK, EquipmentType.BARBELL, ExerciseDifficulty.ADVANCED,
                "Isquios, glúteos", "Convencional",
                "Espalda neutra. Empuje de piso con las piernas.", now));
        Exercise remo = save(Exercise.create(
                "Remo con barra", MuscleGroup.BACK, EquipmentType.BARBELL, ExerciseDifficulty.INTERMEDIATE,
                "Bíceps, posteriores", "Agarre prono",
                "Torso estable. Codo cerca del cuerpo.", now));
        Exercise pressMilitar = save(Exercise.create(
                "Press militar", MuscleGroup.SHOULDERS, EquipmentType.BARBELL, ExerciseDifficulty.INTERMEDIATE,
                "Tríceps, core", "De pie",
                "No hiperextender lumbar. Barra por delante.", now));
        Exercise curl = save(Exercise.create(
                "Curl de bíceps", MuscleGroup.ARMS, EquipmentType.DUMBBELL, ExerciseDifficulty.BEGINNER,
                "Antebrazos", "Con mancuernas",
                "Codos fijos. Evitar balanceo del torso.", now));
        Exercise plancha = save(Exercise.create(
                "Plancha", MuscleGroup.CORE, EquipmentType.BODYWEIGHT, ExerciseDifficulty.BEGINNER,
                "Hombros, glúteos", "Isométrico",
                "Cadera alineada. Respiración continua.", now));
        save(Exercise.create(
                "Bici estática", MuscleGroup.CARDIO, EquipmentType.CARDIO_MACHINE, ExerciseDifficulty.BEGINNER,
                null, "Resistencia moderada",
                "Cadencia estable. Ajustar asiento a la cadera.", now));

        List<Client> clients = clientRepository.list(ClientListScope.ACTIVE);
        Long trainerId = userRepository.findActiveByUsername("juan_prof")
                .or(() -> userRepository.findActiveByUsername("admin"))
                .map(u -> u.getId())
                .orElse(null);
        if (trainerId == null || clients.isEmpty()) {
            log.info("Catálogo de ejercicios demo creado (sin rutinas: falta entrenador o clientes).");
            return;
        }

        Client first = clients.get(0);
        trainingService.createRoutine(new RoutineRequest(
                first.getId(),
                "Hipertrofia — Full body A",
                "Hipertrofia",
                "4 series × 8-10 reps. Descanso 90s.",
                RoutineStatus.ACTIVE,
                null,
                List.of(
                        new RoutineItemRequest(press.getId(), 4, "8-10", 90, "Barra", null),
                        new RoutineItemRequest(sentadilla.getId(), 4, "8-10", 120, null, null),
                        new RoutineItemRequest(remo.getId(), 4, "8-10", 90, null, null),
                        new RoutineItemRequest(plancha.getId(), 3, "40s", 60, "Peso corporal", null)
                )
        ), trainerId);

        if (clients.size() > 1) {
            Client second = clients.get(1);
            trainingService.createRoutine(new RoutineRequest(
                    second.getId(),
                    "Principiante — Máquinas",
                    "Técnica / iniciación",
                    "Técnica controlada. 3 series × 12.",
                    RoutineStatus.SCHEDULED,
                    LocalDate.now(clock).plusDays(7),
                    List.of(
                            new RoutineItemRequest(sentadilla.getId(), 3, "12", 90, "Ligera", null),
                            new RoutineItemRequest(pressMilitar.getId(), 3, "12", 75, null, null),
                            new RoutineItemRequest(curl.getId(), 3, "12", 60, null, null),
                            new RoutineItemRequest(pesoMuerto.getId(), 3, "10", 120, "Técnica", "Priorizar forma")
                    )
            ), trainerId);

            trainingService.createRoutine(new RoutineRequest(
                    second.getId(),
                    "Borrador fuerza",
                    "Fuerza",
                    "Pendiente de cerrar con el cliente.",
                    RoutineStatus.DRAFT,
                    null,
                    List.of(new RoutineItemRequest(pesoMuerto.getId(), 5, "5", 180, null, null))
            ), trainerId);
        }
        log.info("Datos demo de entrenamiento creados.");
    }

    private Exercise save(Exercise exercise) {
        return exerciseRepository.save(exercise);
    }
}
