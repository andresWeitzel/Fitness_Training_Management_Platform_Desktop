package com.fitnesstraining.memberships.model;

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
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "client_memberships")
public class ClientMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected ClientMembership() {
    }

    public static ClientMembership assign(
            Client client,
            MembershipPlan plan,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            OffsetDateTime now) {
        ClientMembership membership = new ClientMembership();
        membership.client = client;
        membership.plan = plan;
        membership.startsAt = startsAt;
        membership.endsAt = endsAt;
        membership.status = MembershipStatus.ACTIVE;
        membership.createdAt = now;
        membership.updatedAt = now;
        return membership;
    }

    public void cancel(OffsetDateTime now) {
        this.status = MembershipStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }

    public void markExpired(OffsetDateTime now) {
        this.status = MembershipStatus.EXPIRED;
        this.updatedAt = now;
    }

    public void renew(OffsetDateTime newEndsAt, OffsetDateTime now) {
        this.endsAt = newEndsAt;
        this.status = MembershipStatus.ACTIVE;
        this.cancelledAt = null;
        this.updatedAt = now;
    }

    public void changePlan(
            MembershipPlan plan,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            OffsetDateTime now) {
        this.plan = plan;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = MembershipStatus.ACTIVE;
        this.cancelledAt = null;
        this.updatedAt = now;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !endsAt.isAfter(now);
    }

    public MembershipStatus effectiveStatus(OffsetDateTime now) {
        if (status == MembershipStatus.CANCELLED) {
            return MembershipStatus.CANCELLED;
        }
        if (isExpired(now)) {
            return MembershipStatus.EXPIRED;
        }
        return MembershipStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public MembershipPlan getPlan() {
        return plan;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }
}
