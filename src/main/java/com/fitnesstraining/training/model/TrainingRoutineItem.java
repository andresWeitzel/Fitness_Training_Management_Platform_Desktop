package com.fitnesstraining.training.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_routine_items")
public class TrainingRoutineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private TrainingRoutine routine;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column
    private Integer sets;

    @Column(length = 40)
    private String reps;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(name = "load_note", length = 80)
    private String loadNote;

    @Column(length = 300)
    private String notes;

    protected TrainingRoutineItem() {
    }

    public static TrainingRoutineItem of(
            Long exerciseId,
            Integer sets,
            String reps,
            Integer restSeconds,
            String loadNote,
            String notes) {
        TrainingRoutineItem item = new TrainingRoutineItem();
        item.exerciseId = exerciseId;
        item.sets = sets;
        item.reps = reps;
        item.restSeconds = restSeconds;
        item.loadNote = loadNote;
        item.notes = notes;
        return item;
    }

    void attach(TrainingRoutine routine, int sortOrder) {
        this.routine = routine;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Integer getSets() {
        return sets;
    }

    public String getReps() {
        return reps;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public String getLoadNote() {
        return loadNote;
    }

    public String getNotes() {
        return notes;
    }
}
