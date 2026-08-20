package com.fitnesstraining.memberships.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MembershipPlan() {
    }

    public static MembershipPlan create(
            String name,
            String description,
            int durationDays,
            BigDecimal price,
            OffsetDateTime now) {
        MembershipPlan plan = new MembershipPlan();
        plan.name = name;
        plan.description = description;
        plan.durationDays = durationDays;
        plan.price = price == null ? BigDecimal.ZERO : price;
        plan.active = true;
        plan.createdAt = now;
        plan.updatedAt = now;
        return plan;
    }

    public void update(String name, String description, int durationDays, BigDecimal price, boolean active, OffsetDateTime now) {
        this.name = name;
        this.description = description;
        this.durationDays = durationDays;
        this.price = price == null ? BigDecimal.ZERO : price;
        this.active = active;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public BigDecimal getPrice() {
        return price;
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
