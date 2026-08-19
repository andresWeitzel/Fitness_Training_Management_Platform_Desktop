package com.fitnesstraining.members.service;

import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientSummary;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.dto.CredentialView;
import com.fitnesstraining.members.model.AccessCredential;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.CredentialType;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.members.validation.ClientValidator;
import com.fitnesstraining.shared.exception.ValidationException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;

public class ClientService {

    public static final Period CARD_VALIDITY = Period.ofYears(1);

    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final Clock clock;

    public ClientService(
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            Clock clock) {
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.clock = clock;
    }

    public long countActiveClients() {
        return clientRepository.countActive();
    }

    public List<ClientSummary> list(String query) {
        List<Client> clients = query == null || query.isBlank()
                ? clientRepository.findAllActiveRecords()
                : clientRepository.search(query);
        return clients.stream()
                .map(client -> ClientSummary.from(
                        client,
                        credentialRepository.findClientNumber(client.getId()).orElse("")))
                .toList();
    }

    public ClientView get(Long id) {
        Client client = requireClient(id);
        return toView(client);
    }

    public ClientView create(ClientRequest request) {
        ClientRequest data = ClientValidator.normalizeAndValidate(request);
        if (clientRepository.existsDocument(data.documentNumber(), null)) {
            throw new ValidationException("Ya existe un cliente con ese documento.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Client client = Client.register(
                data.documentNumber(),
                data.firstName(),
                data.lastName(),
                data.email(),
                data.phone(),
                data.address(),
                now
        );
        Client saved = clientRepository.save(client);
        AccessCredential number = AccessCredential.issue(
                CredentialType.CLIENT_NUMBER,
                credentialRepository.nextCode(CredentialType.CLIENT_NUMBER),
                now,
                null
        );
        credentialRepository.addToClient(saved.getId(), number);
        return get(saved.getId());
    }

    public ClientView update(Long id, ClientRequest request) {
        ClientRequest data = ClientValidator.normalizeAndValidate(request);
        Client client = requireClient(id);
        if (clientRepository.existsDocument(data.documentNumber(), id)) {
            throw new ValidationException("Ya existe un cliente con ese documento.");
        }
        if (!client.getDocumentNumber().equalsIgnoreCase(data.documentNumber())) {
            throw new ValidationException("El documento no se puede modificar. Da de baja y registra un alta nueva.");
        }
        client.updateProfile(
                data.firstName(),
                data.lastName(),
                data.email(),
                data.phone(),
                data.address(),
                OffsetDateTime.now(clock)
        );
        clientRepository.save(client);
        return get(id);
    }

    public void deactivate(Long id) {
        Client client = requireClient(id);
        client.deactivate(OffsetDateTime.now(clock));
        clientRepository.save(client);
        credentialRepository.deactivateAllForClient(id);
    }

    public ClientView issueCard(Long clientId) {
        Client client = requireClient(clientId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        AccessCredential existing = credentialRepository
                .findActiveByClientAndType(client.getId(), CredentialType.CARD)
                .orElse(null);
        if (existing != null && existing.isUsable(now)) {
            throw new ValidationException("El cliente ya tiene un carnet vigente. Use renovar.");
        }
        if (existing != null) {
            existing.renew(now, now.plus(CARD_VALIDITY));
            credentialRepository.save(existing);
            return get(clientId);
        }
        AccessCredential card = AccessCredential.issue(
                CredentialType.CARD,
                credentialRepository.nextCode(CredentialType.CARD),
                now,
                now.plus(CARD_VALIDITY)
        );
        credentialRepository.addToClient(clientId, card);
        return get(clientId);
    }

    public ClientView renewCard(Long clientId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        AccessCredential card = credentialRepository
                .findActiveByClientAndType(clientId, CredentialType.CARD)
                .orElseThrow(() -> new ValidationException("El cliente no tiene un carnet para renovar. Emita uno nuevo."));
        card.renew(now, now.plus(CARD_VALIDITY));
        credentialRepository.save(card);
        return get(clientId);
    }

    public ClientView issueQr(Long clientId) {
        requireClient(clientId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (credentialRepository.findActiveByClientAndType(clientId, CredentialType.QR)
                .filter(credential -> credential.isUsable(now))
                .isPresent()) {
            throw new ValidationException("El cliente ya tiene un código QR vigente.");
        }
        AccessCredential qr = AccessCredential.issue(
                CredentialType.QR,
                credentialRepository.nextCode(CredentialType.QR),
                now,
                null
        );
        credentialRepository.addToClient(clientId, qr);
        return get(clientId);
    }

    private Client requireClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Cliente no encontrado."));
    }

    private ClientView toView(Client client) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<CredentialView> credentials = credentialRepository.findByClientId(client.getId()).stream()
                .map(credential -> CredentialView.from(credential, now))
                .toList();
        return ClientView.from(client, credentials);
    }
}
