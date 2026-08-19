package com.fitnesstraining.members.dto;

public record ClientRequest(
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address
) {
}
