package com.fitnesstraining.members.model;

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
@Table(name = "access_credentials")
public class AccessCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CredentialType type;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AccessCredential() {
    }

    public static AccessCredential issue(
            CredentialType type,
            String code,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {
        AccessCredential credential = new AccessCredential();
        credential.type = type;
        credential.code = code;
        credential.issuedAt = issuedAt;
        credential.expiresAt = expiresAt;
        credential.active = true;
        credential.createdAt = issuedAt;
        return credential;
    }

    public void assignTo(Client client) {
        this.client = client;
    }

    public void renew(OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isUsable(OffsetDateTime now) {
        return active && !isExpired(now);
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public CredentialType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }
}
