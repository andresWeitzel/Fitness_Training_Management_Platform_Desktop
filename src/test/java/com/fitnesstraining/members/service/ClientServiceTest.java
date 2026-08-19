package com.fitnesstraining.members.service;

import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccessCredentialRepository credentialRepository;

    private ClientService clientService;
    private final AtomicReference<Client> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, credentialRepository, CLOCK);
    }

    @Test
    void rejectsIncompleteClient() {
        assertThrows(ValidationException.class, () ->
                clientService.create(new ClientRequest("12345678", "  ", "Lopez", null, null, null)));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateDocument() {
        when(clientRepository.existsDocument("12345678", null)).thenReturn(true);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                clientService.create(validRequest()));
        assertEquals("Ya existe un cliente con ese documento.", ex.getMessage());
    }

    @Test
    void createAssignsClientNumber() {
        when(clientRepository.existsDocument("12345678", null)).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = withId(invocation.getArgument(0), 10L);
            stored.set(client);
            return client;
        });
        when(credentialRepository.nextCode(CredentialType.CLIENT_NUMBER)).thenReturn("CLI-000010");
        when(clientRepository.findById(10L)).thenAnswer(invocation -> Optional.of(stored.get()));
        AccessCredential number = AccessCredential.issue(
                CredentialType.CLIENT_NUMBER, "CLI-000010", OffsetDateTime.now(CLOCK), null);
        when(credentialRepository.findByClientId(10L)).thenReturn(List.of(number));

        ClientView view = clientService.create(validRequest());

        assertEquals(10L, view.id());
        assertEquals("CLI-000010", view.credentials().getFirst().code());
        assertEquals(CredentialType.CLIENT_NUMBER, view.credentials().getFirst().type());
        ArgumentCaptor<AccessCredential> captor = ArgumentCaptor.forClass(AccessCredential.class);
        verify(credentialRepository).addToClient(eq(10L), captor.capture());
        assertEquals("CLI-000010", captor.getValue().getCode());
    }

    @Test
    void issueCardFailsIfAlreadyValid() {
        Client client = withId(Client.register("12345678", "Ana", "Lopez", null, null, null, OffsetDateTime.now(CLOCK)), 10L);
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        AccessCredential card = AccessCredential.issue(
                CredentialType.CARD, "CARD-000001", OffsetDateTime.now(CLOCK), OffsetDateTime.now(CLOCK).plusYears(1));
        when(credentialRepository.findActiveByClientAndType(10L, CredentialType.CARD)).thenReturn(Optional.of(card));

        ValidationException ex = assertThrows(ValidationException.class, () -> clientService.issueCard(10L));
        assertTrue(ex.getMessage().contains("carnet vigente"));
        verify(credentialRepository, never()).addToClient(eq(10L), any());
    }

    @Test
    void deactivateTurnsOffCredentials() {
        Client client = withId(Client.register("12345678", "Ana", "Lopez", null, null, null, OffsetDateTime.now(CLOCK)), 10L);
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));

        clientService.deactivate(10L);

        assertTrue(client.isDeleted());
        verify(credentialRepository).deactivateAllForClient(10L);
        verify(clientRepository).save(client);
    }

    private static ClientRequest validRequest() {
        return new ClientRequest("12345678", "Ana", "Lopez", "ana@mail.com", "111", "Calle 1");
    }

    private static Client withId(Client client, Long id) {
        try {
            Field field = Client.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(client, id);
            return client;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
