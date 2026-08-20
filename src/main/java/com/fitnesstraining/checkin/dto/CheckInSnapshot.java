package com.fitnesstraining.checkin.dto;

public record CheckInSnapshot(
        int entriesToday,
        int uniqueClientsToday
) {
}
