package com.fitnesstraining.members.service;

import com.fitnesstraining.members.dto.ClientRequest;
import com.fitnesstraining.members.dto.ClientView;
import com.fitnesstraining.members.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(ClientDemoSeeder.class);

    /**
     * Catálogo fijo de prueba (~2× el set original). Se crean los que falten por DNI.
     * Variedad: activos/bajas, carnet, QR, ambos o solo n° de cliente; emails/teléfonos/direcciones reales.
     */
    private static final DemoClient[] DEMO_CLIENTS = {
            // --- set original ---
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
                    "Alsina 340", true, false, false),

            // --- ampliación (≥2×): más carnets, QR y perfiles mixtos ---
            new DemoClient("31433444", "Florencia", "Benítez", "flor.benitez@email.com", "1144557788",
                    "Av. Libertador 2200", true, true, false),
            new DemoClient("31544555", "Hernán", "Vega", "hernan.vega@email.com", "1155668899",
                    "Scalabrini Ortiz 480", true, false, false),
            new DemoClient("31655666", "Paula", "Molina", "paula.molina@email.com", "1166779900",
                    "Cabildo 1550", false, true, false),
            new DemoClient("31766777", "Federico", "Navarro", "fede.navarro@email.com", "1177880011",
                    "Santa Fe 3200", true, true, false),
            new DemoClient("31877888", "Agustina", "Silva", "agus.silva@email.com", "1188991122",
                    "Thames 890", false, false, false),
            new DemoClient("31988999", "Bruno", "Acosta", "bruno.acosta@email.com", "1199002233",
                    "Honduras 1450", true, false, false),
            new DemoClient("32099000", "Carolina", "Ibáñez", "caro.ibanez@email.com", "1100113344",
                    "Uriburu 670", false, true, false),
            new DemoClient("32100112", "Emiliano", "Ríos", "emi.rios@email.com", "1111224455",
                    "Av. Córdoba 4100", true, true, false),
            new DemoClient("32211223", "Jimena", "Paredes", "jimena.paredes@email.com", "1122335566",
                    "Malabia 1120", true, true, false),
            new DemoClient("32322334", "Tomás", "Aguirre", "tomas.aguirre@email.com", "1133446677",
                    "Arenales 980", false, false, false),
            new DemoClient("32433445", "Rocío", "Medina", "rocio.medina@email.com", "1144557789",
                    "Av. Callao 750", true, false, false),
            new DemoClient("32544556", "Santiago", "Herrera", "santi.herrera@email.com", "1155668890",
                    "Gorriti 2100", false, true, false),
            new DemoClient("32655667", "Melina", "Quiroga", "melina.quiroga@email.com", "1166779901",
                    "Av. Santa Fe 1800", true, true, false),
            new DemoClient("32766778", "Ignacio", "Blanco", "nacho.blanco@email.com", "1177880012",
                    "Fitz Roy 560", true, false, true),
            new DemoClient("32877889", "Bianca", "Soria", "bianca.soria@email.com", "1188991123",
                    "Costa Rica 1340", false, true, true),
            new DemoClient("32988990", "Matías", "Cabrera", "mati.cabrera@email.com", "1199002234",
                    "Av. Rivadavia 5800", true, true, false),
            new DemoClient("33099001", "Elena", "Fuentes", "elena.fuentes@email.com", "1100113345",
                    "Paraguay 2200", false, false, false),
            new DemoClient("33100113", "Gabriel", "Ortiz", "gabriel.ortiz@email.com", "1111224456",
                    "Av. Corrientes 3500", true, true, false),
            new DemoClient("33211224", "Natalia", "Reyes", "natalia.reyes@email.com", "1122335567",
                    "Charcas 4100", true, false, false),
            new DemoClient("33322335", "Facundo", "Morales", "facu.morales@email.com", "1133446678",
                    "Av. del Libertador 5500", false, true, false),
            new DemoClient("33433446", "Abril", "Campos", "abril.campos@email.com", "1144557790",
                    "Juramento 2600", true, true, false),
            new DemoClient("33544557", "Sebastián", "Luna", "seba.luna@email.com", "1155668891",
                    "Av. Cabildo 3200", false, false, false),
            new DemoClient("33655668", "Milagros", "Paz", "mila.paz@email.com", "1166779902",
                    "Monroe 1800", true, true, false)
    };

    private final ClientRepository clientRepository;
    private final ClientService clientService;

    public ClientDemoSeeder(ClientRepository clientRepository, ClientService clientService) {
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    public void seedIfEmpty() {
        int pruned = clientRepository.pruneDuplicateInactiveClients();
        if (pruned > 0) {
            log.info("Se eliminaron {} clientes de baja duplicados (basura de seed anterior).", pruned);
        }

        int created = 0;
        int cards = 0;
        int qrs = 0;
        for (DemoClient demo : DEMO_CLIENTS) {
            // Incluye bajas: si no, cada arranque recreaba los demos dados de baja.
            if (clientRepository.existsDocumentIncludingInactive(demo.documentNumber())) {
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
                    cards++;
                }
                if (demo.issueQr()) {
                    clientService.issueQr(view.id());
                    qrs++;
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
            log.info(
                    "Clientes demo ya presentes (activos {}, bajas {}). Catálogo definido: {}.",
                    clientRepository.countActive(),
                    clientRepository.countInactive(),
                    DEMO_CLIENTS.length);
            return;
        }
        log.info(
                "Clientes demo creados: {} (carnet {}, QR {}). Activos {}, bajas {}. Catálogo {}.",
                created,
                cards,
                qrs,
                clientRepository.countActive(),
                clientRepository.countInactive(),
                DEMO_CLIENTS.length);
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
