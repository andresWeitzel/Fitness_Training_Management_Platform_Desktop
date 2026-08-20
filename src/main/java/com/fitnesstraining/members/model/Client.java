package com.fitnesstraining.members.model;

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
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(name = "photo_path")
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Client() {
    }

    public static Client register(
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            OffsetDateTime now) {
        Client client = new Client();
        client.documentNumber = documentNumber;
        client.firstName = firstName;
        client.lastName = lastName;
        client.email = email;
        client.phone = phone;
        client.address = address;
        client.status = ClientStatus.ACTIVE;
        client.createdAt = now;
        client.updatedAt = now;
        return client;
    }

    public void updateProfile(
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            OffsetDateTime now) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.updatedAt = now;
    }

    public void deactivate(OffsetDateTime now) {
        this.status = ClientStatus.INACTIVE;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public String fullName() {
        return lastName + ", " + firstName;
    }
}
