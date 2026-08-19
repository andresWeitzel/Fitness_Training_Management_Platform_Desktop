package com.fitnesstraining.app;

import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.auth.service.AuthService;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.auth.service.DevDataSeeder;
import com.fitnesstraining.auth.service.PasswordHasher;
import com.fitnesstraining.controller.ClientsController;
import com.fitnesstraining.controller.DashboardController;
import com.fitnesstraining.controller.DbSetupController;
import com.fitnesstraining.controller.LoginController;
import com.fitnesstraining.controller.PlaceholderController;
import com.fitnesstraining.controller.ShellController;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.members.service.ClientDemoSeeder;
import com.fitnesstraining.members.service.ClientQueryService;
import com.fitnesstraining.members.service.ClientService;
import com.fitnesstraining.shared.config.AppProperties;
import com.fitnesstraining.shared.config.DatabaseBootstrap;
import com.fitnesstraining.shared.config.DatabaseConfigStore;
import com.fitnesstraining.shared.config.DatabaseSettings;
import com.fitnesstraining.shared.config.FlywayMigrator;
import com.fitnesstraining.shared.config.PersistenceManager;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

public class AppContext {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    private final Stage stage;
    private final AppProperties properties = AppProperties.loadClasspath();
    private final DatabaseConfigStore configStore = new DatabaseConfigStore();
    private final DatabaseBootstrap databaseBootstrap = new DatabaseBootstrap();
    private final FlywayMigrator flywayMigrator = new FlywayMigrator();
    private final SessionContext sessionContext = new SessionContext();
    private final PasswordHasher passwordHasher = new PasswordHasher();
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final ViewLoader viewLoader = new ViewLoader(this::createController);
    private final SceneNavigator navigator;

    private PersistenceManager persistenceManager;
    private AuthService authService;
    private ClientQueryService clientQueryService;
    private ClientService clientService;

    public AppContext(Stage stage) {
        this.stage = stage;
        this.navigator = new SceneNavigator(stage, viewLoader);
    }

    public void start() {
        stage.setTitle(properties.get("app.name", "Fitness Training Management Platform"));
        stage.setMinWidth(880);
        stage.setMinHeight(640);
        configStore.load()
                .ifPresentOrElse(this::connectAndShowLogin, () -> navigator.showDbSetup(null));
        stage.show();
    }

    public void connectAndShowLogin(DatabaseSettings settings) {
        try {
            shutdownPersistence();
            databaseBootstrap.ensureDatabaseExists(settings);
            flywayMigrator.migrate(settings);
            boolean showSql = Boolean.parseBoolean(properties.get("hibernate.show_sql", "false"));
            persistenceManager = new PersistenceManager(settings, showSql);
            UserRepository userRepository = new UserRepository(persistenceManager);
            ClientRepository clientRepository = new ClientRepository(persistenceManager);
            AccessCredentialRepository credentialRepository = new AccessCredentialRepository(persistenceManager);
            authService = new AuthService(userRepository, passwordHasher);
            clientQueryService = new ClientQueryService(clientRepository);
            clientService = new ClientService(clientRepository, credentialRepository, Clock.systemDefaultZone());
            new DevDataSeeder(userRepository, passwordHasher).seedIfEmpty();
            new ClientDemoSeeder(clientRepository, clientService).seedIfEmpty();
            navigator.showLogin();
        } catch (Exception ex) {
            log.error("No se pudo inicializar PostgreSQL", ex);
            navigator.showDbSetup(ex.getMessage());
        }
    }

    public void logout() {
        sessionContext.clear();
        navigator.showLogin();
    }

    public void openDatabaseSetup() {
        navigator.showDbSetup(null);
    }

    public void shutdown() {
        shutdownPersistence();
    }

    public AppProperties properties() {
        return properties;
    }

    public DatabaseConfigStore configStore() {
        return configStore;
    }

    Object createController(Class<?> type) {
        if (type == DbSetupController.class) {
            return new DbSetupController(this, configStore, databaseBootstrap, properties);
        }
        if (type == LoginController.class) {
            return new LoginController(authService, sessionContext, navigator, this);
        }
        if (type == ShellController.class) {
            return new ShellController(sessionContext, authorizationService, navigator, this);
        }
        if (type == DashboardController.class) {
            return new DashboardController(sessionContext, clientQueryService);
        }
        if (type == ClientsController.class) {
            return new ClientsController(clientService, sessionContext, authorizationService);
        }
        if (type == PlaceholderController.class) {
            return new PlaceholderController();
        }
        throw new IllegalArgumentException("No hay factory para " + type.getName());
    }

    private void shutdownPersistence() {
        if (persistenceManager != null) {
            persistenceManager.close();
            persistenceManager = null;
        }
    }
}
