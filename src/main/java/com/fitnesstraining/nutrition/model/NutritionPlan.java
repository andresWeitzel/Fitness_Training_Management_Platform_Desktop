package com.fitnesstraining.nutrition.model;

import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.members.model.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nutrition_plans")
public class NutritionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String objectives;

    @Column(name = "meal_guidance", length = 4000)
    private String mealGuidance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NutritionPlanStatus status = NutritionPlanStatus.DRAFT;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected NutritionPlan() {
    }

    public static NutritionPlan create(
            Client client,
            User createdBy,
            String title,
            String objectives,
            String mealGuidance,
            NutritionPlanStatus status,
            LocalDate validFrom,
            LocalDate validUntil,
            String notes) {
        NutritionPlan plan = new NutritionPlan();
        plan.client = client;
        plan.createdBy = createdBy;
        plan.title = title;
        plan.objectives = objectives;
        plan.mealGuidance = mealGuidance;
        plan.status = status == null ? NutritionPlanStatus.DRAFT : status;
        plan.validFrom = validFrom;
        plan.validUntil = validUntil;
        plan.notes = notes;
        return plan;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getMealGuidance() {
        return mealGuidance;
    }

    public void setMealGuidance(String mealGuidance) {
        this.mealGuidance = mealGuidance;
    }

    public NutritionPlanStatus getStatus() {
        return status;
    }

    public void setStatus(NutritionPlanStatus status) {
        this.status = status;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
