package com.fitnesstraining.checkin.model;

import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.CredentialType;
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
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id")
    private AccessCredential credential;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", length = 30)
    private CredentialType credentialType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 30)
    private AccessMode accessMode;

    @Column(name = "checked_in_at", nullable = false)
    private OffsetDateTime checkedInAt;

    @Column(length = 500)
    private String notes;

    protected CheckIn() {
    }

    public static CheckIn register(
            Client client,
            AccessCredential credential,
            AccessMode accessMode,
            String notes,
            OffsetDateTime now) {
        CheckIn checkIn = new CheckIn();
        checkIn.client = client;
        checkIn.credential = credential;
        checkIn.credentialType = credential == null ? null : credential.getType();
        checkIn.accessMode = accessMode;
        checkIn.checkedInAt = now;
        checkIn.notes = notes;
        return checkIn;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public AccessCredential getCredential() {
        return credential;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public OffsetDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public String getNotes() {
        return notes;
    }
}
