package com.fitnesstraining.members.dto;

import java.util.List;

public record DashboardSnapshot(
        long activeClients,
        long inactiveClients,
        long activeCards,
        long activeQrCodes,
        List<ClientSummary> recentClients
) {
}
