package com.fitnesstraining.members.service;

import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(ClientDemoSeeder.class);

    /**
     * Catálogo fijo de prueba. Se crean los que falten por DNI (útil si la base ya tenía el seed chico).
     * Estados: activos/bajas, con carnet, QR, ambos o solo n° de cliente.
     */
    private static final DemoClient[] DEMO_CLIENTS = {
            new DemoClient("30111222", "Carlos", "García", "carlos.garcia@email.com", "1123456789",
                    "Av. Principal 100", true, true, false),
            new DemoClient("30222333", "María", "López", "maria.lopez@email.com", "1198765432",
                    "Calle 45 200", false, false, false),
            new DemoClient("30333444", "Ana", "Rodríguez", "ana.rodriguez@email.com", "1144556677",
                    "Calle B 300", true, false, false),
            new DemoClient("30444555", "Luis", "Fernández", "luis.fernandez@email.com", "1155667788",
                    "Av. San Martín 450", false, true, false),
            new DemoClient("30555666", "Sofía", "Martínez", "sofia.martinez@email.com", "1166778899",
                    "Belgrano 120", true, true, false),
            new DemoClient("30666777", "Diego", "Suárez", "diego.suarez@email.com", "1177889900",
                    "Mitre 88", false, false, false),
            new DemoClient("30777888", "Valentina", "Ruiz", "valentina.ruiz@email.com", "1188990011",
                    "Lavalle 2100", true, true, true),
            new DemoClient("30888999", "Martín", "Pérez", "martin.perez@email.com", "1199001122",
                    "Córdoba 560", true, false, true),
            new DemoClient("30999000", "Lucía", "Gómez", "lucia.gomez@email.com", "1100112233",
                    "Corrientes 900", false, true, false),
            new DemoClient("31100111", "Julián", "Torres", "julian.torres@email.com", "1111223344",
                    "Rivadavia 1500", true, true, false),
            new DemoClient("31211222", "Camila", "Díaz", "camila.diaz@email.com", "1122334455",
                    "Independencia 77", false, false, true),
            new DemoClient("31322333", "Nicolás", "Castro", "nicolas.castro@email.com", "1133445566",
                    "Alsina 340", true, false, false)
    };

    private final ClientRepository clientRepository;
    private final ClientService clientService;

    public ClientDemoSeeder(ClientRepository clientRepository, ClientService clientService) {
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    public void seedIfEmpty() {
        int created = 0;
        for (DemoClient demo : DEMO_CLIENTS) {
            if (clientRepository.existsDocument(demo.documentNumber(), null)) {
                continue;
            }
            try {
                ClientView view = clientService.create(new ClientRequest(
                        demo.documentNumber(),
                        demo.firstName(),
                        demo.lastName(),
                        demo.email(),
                        demo.phone(),
                        demo.address()));
                if (demo.issueCard()) {
                    clientService.issueCard(view.id());
                }
                if (demo.issueQr()) {
                    clientService.issueQr(view.id());
                }
                if (demo.deactivate()) {
                    clientService.deactivate(view.id());
                }
                created++;
            } catch (RuntimeException ex) {
                log.warn("No se pudo crear cliente demo {}: {}", demo.documentNumber(), ex.getMessage());
            }
        }

        if (created == 0) {
            log.info("Clientes demo ya presentes (total {}).", clientRepository.countAll());
            return;
        }
        log.info(
                "Clientes demo creados: {} (total {}). Activos/bajas y credenciales variadas.",
                created,
                clientRepository.countAll());
    }

    private record DemoClient(
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            boolean issueCard,
            boolean issueQr,
            boolean deactivate
    ) {
    }
}
