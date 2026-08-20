package com.fitnesstraining.members.service;

import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientQueryServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccessCredentialRepository credentialRepository;

    @Test
    void snapshotUsesLiveClientAndCredentialCounts() {
        OffsetDateTime now = OffsetDateTime.of(2026, 8, 19, 12, 0, 0, 0, ZoneOffset.UTC);
        Client client = Client.register("30111222", "Ana", "Perez", null, null, null, now);
        when(clientRepository.countActive()).thenReturn(4L);
        when(clientRepository.countInactive()).thenReturn(1L);
        when(clientRepository.findRecent(6)).thenReturn(List.of(client));
        when(credentialRepository.countActiveByType(CredentialType.CARD)).thenReturn(3L);
        when(credentialRepository.countActiveByType(CredentialType.QR)).thenReturn(2L);
        when(credentialRepository.findClientNumber(null)).thenReturn(Optional.of("CLI-000001"));

        var snapshot = new ClientQueryService(clientRepository, credentialRepository).loadSnapshot();

        assertEquals(4, snapshot.activeClients());
        assertEquals(1, snapshot.inactiveClients());
        assertEquals(3, snapshot.activeCards());
        assertEquals(2, snapshot.activeQrCodes());
        assertEquals("Perez, Ana", snapshot.recentClients().getFirst().fullName());
    }
}
