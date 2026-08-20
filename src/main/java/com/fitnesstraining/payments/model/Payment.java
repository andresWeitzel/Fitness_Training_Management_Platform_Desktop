package com.fitnesstraining.payments.model;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.memberships.model.ClientMembership;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_membership_id")
    private ClientMembership clientMembership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod method;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected Payment() {
    }

    public static Payment register(
            Client client,
            ClientMembership membership,
            PaymentType type,
            BigDecimal amount,
            PaymentMethod method,
            OffsetDateTime dueAt,
            boolean markAsPaid,
            String notes,
            OffsetDateTime now) {
        Payment payment = new Payment();
        payment.client = client;
        payment.clientMembership = membership;
        payment.type = type;
        payment.amount = amount;
        payment.notes = notes;
        payment.dueAt = dueAt;
        payment.createdAt = now;
        payment.updatedAt = now;
        if (markAsPaid) {
            payment.status = PaymentStatus.PAID;
            payment.method = method;
            payment.paidAt = now;
        } else {
            payment.status = PaymentStatus.PENDING;
            payment.method = method;
        }
        return payment;
    }

    public void markPaid(PaymentMethod method, OffsetDateTime now) {
        this.status = PaymentStatus.PAID;
        this.method = method;
        this.paidAt = now;
        this.updatedAt = now;
        this.cancelledAt = null;
    }

    public void cancel(OffsetDateTime now) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }

    public boolean isOverdue(OffsetDateTime now) {
        return status == PaymentStatus.PENDING
                && dueAt != null
                && !dueAt.isAfter(now);
    }

    public PaymentStatus effectiveStatus(OffsetDateTime now) {
        if (status == PaymentStatus.PENDING && isOverdue(now)) {
            return PaymentStatus.PENDING;
        }
        return status;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public ClientMembership getClientMembership() {
        return clientMembership;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }
}
