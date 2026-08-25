package com.fitnesstraining.training.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_routines")
public class TrainingRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "trainer_user_id", nullable = false)
    private Long trainerUserId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 80)
    private String focus;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoutineStatus status = RoutineStatus.ACTIVE;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<TrainingRoutineItem> items = new ArrayList<>();

    protected TrainingRoutine() {
    }

    public static TrainingRoutine create(
            Long clientId,
            Long trainerUserId,
            String title,
            String focus,
            String notes,
            RoutineStatus status,
            LocalDate startsOn,
            OffsetDateTime now) {
        TrainingRoutine routine = new TrainingRoutine();
        routine.clientId = clientId;
        routine.trainerUserId = trainerUserId;
        routine.title = title;
        routine.focus = focus;
        routine.notes = notes;
        routine.status = status == null ? RoutineStatus.ACTIVE : status;
        routine.startsOn = startsOn;
        routine.createdAt = now;
        routine.updatedAt = now;
        return routine;
    }

    public void update(
            String title,
            String focus,
            String notes,
            RoutineStatus status,
            LocalDate startsOn,
            Long trainerUserId,
            OffsetDateTime now) {
        this.title = title;
        this.focus = focus;
        this.notes = notes;
        if (status != null) {
            this.status = status;
        }
        this.startsOn = startsOn;
        if (trainerUserId != null) {
            this.trainerUserId = trainerUserId;
        }
        this.updatedAt = now;
    }

    public void replaceItems(List<TrainingRoutineItem> nextItems, OffsetDateTime now) {
        this.items.clear();
        if (nextItems != null) {
            int order = 0;
            for (TrainingRoutineItem item : nextItems) {
                item.attach(this, order++);
                this.items.add(item);
            }
        }
        this.updatedAt = now;
    }

    public void changeStatus(RoutineStatus status, OffsetDateTime now) {
        this.status = status;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getTrainerUserId() {
        return trainerUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getFocus() {
        return focus;
    }

    public String getNotes() {
        return notes;
    }

    public RoutineStatus getStatus() {
        return status;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<TrainingRoutineItem> getItems() {
        return items;
    }
}
