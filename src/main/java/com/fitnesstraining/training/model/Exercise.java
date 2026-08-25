package com.fitnesstraining.training.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false, length = 40)
    private MuscleGroup muscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EquipmentType equipment = EquipmentType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ExerciseDifficulty difficulty = ExerciseDifficulty.INTERMEDIATE;

    @Column(name = "secondary_muscles", length = 200)
    private String secondaryMuscles;

    @Column(length = 500)
    private String description;

    @Column(name = "technique_notes", length = 1000)
    private String techniqueNotes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Exercise() {
    }

    public static Exercise create(
            String name,
            MuscleGroup muscleGroup,
            EquipmentType equipment,
            ExerciseDifficulty difficulty,
            String secondaryMuscles,
            String description,
            String techniqueNotes,
            OffsetDateTime now) {
        Exercise exercise = new Exercise();
        exercise.name = name;
        exercise.muscleGroup = muscleGroup;
        exercise.equipment = equipment == null ? EquipmentType.OTHER : equipment;
        exercise.difficulty = difficulty == null ? ExerciseDifficulty.INTERMEDIATE : difficulty;
        exercise.secondaryMuscles = secondaryMuscles;
        exercise.description = description;
        exercise.techniqueNotes = techniqueNotes;
        exercise.active = true;
        exercise.createdAt = now;
        exercise.updatedAt = now;
        return exercise;
    }

    public void update(
            String name,
            MuscleGroup muscleGroup,
            EquipmentType equipment,
            ExerciseDifficulty difficulty,
            String secondaryMuscles,
            String description,
            String techniqueNotes,
            boolean active,
            OffsetDateTime now) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment == null ? EquipmentType.OTHER : equipment;
        this.difficulty = difficulty == null ? ExerciseDifficulty.INTERMEDIATE : difficulty;
        this.secondaryMuscles = secondaryMuscles;
        this.description = description;
        this.techniqueNotes = techniqueNotes;
        this.active = active;
        this.updatedAt = now;
    }

    public void deactivate(OffsetDateTime now) {
        this.active = false;
        this.updatedAt = now;
    }

    public void reactivate(OffsetDateTime now) {
        this.active = true;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MuscleGroup getMuscleGroup() {
        return muscleGroup;
    }

    public EquipmentType getEquipment() {
        return equipment;
    }

    public ExerciseDifficulty getDifficulty() {
        return difficulty;
    }

    public String getSecondaryMuscles() {
        return secondaryMuscles;
    }

    public String getDescription() {
        return description;
    }

    public String getTechniqueNotes() {
        return techniqueNotes;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
