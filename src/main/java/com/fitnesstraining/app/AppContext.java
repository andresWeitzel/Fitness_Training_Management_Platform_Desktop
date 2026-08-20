package com.fitnesstraining.app;

import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.auth.service.AuthService;
import com.fitnesstraining.auth.service.AuthorizationService;
import com.fitnesstraining.auth.service.DevDataSeeder;
import com.fitnesstraining.auth.service.PasswordHasher;
import com.fitnesstraining.controller.ClientsController;
import com.fitnesstraining.controller.DashboardController;
import com.fitnesstraining.auth.dto.PendingLoginFill;
import com.fitnesstraining.controller.DemoAccountsController;
import com.fitnesstraining.controller.DbSetupController;
import com.fitnesstraining.controller.LoginController;
import com.fitnesstraining.controller.MembershipsController;
import com.fitnesstraining.controller.PaymentsController;
import com.fitnesstraining.controller.PlaceholderController;
import com.fitnesstraining.controller.ShellController;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.members.service.ClientDemoSeeder;
import com.fitnesstraining.members.service.ClientQueryService;
import com.fitnesstraining.members.service.ClientService;
import com.fitnesstraining.memberships.repository.ClientMembershipRepository;
import com.fitnesstraining.memberships.repository.MembershipPlanRepository;
import com.fitnesstraining.memberships.service.MembershipDemoSeeder;
import com.fitnesstraining.memberships.service.MembershipService;
import com.fitnesstraining.payments.repository.PaymentRepository;
import com.fitnesstraining.payments.service.PaymentDemoSeeder;
import com.fitnesstraining.payments.service.PaymentService;
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
import java.util.Optional;

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
    private MembershipService membershipService;
    private PaymentService paymentService;
    private ShellController shellController;
    private PendingLoginFill pendingLogin;
    private String pendingConnectionError;
    private DbConnectionSnapshot connectionSnapshot = DbConnectionSnapshot.unknown();

    public AppContext(Stage stage) {
        this.stage = stage;
        this.navigator = new SceneNavigator(stage, viewLoader);
    }

    public void start() {
        stage.setTitle(properties.get("app.name", "Fitness Training Management Platform"));
        configStore.load()
                .ifPresentOrElse(this::connectAndShowLogin, () -> navigator.showDbSetup(null));
        stage.show();
    }

    public void connectAndShowLogin(DatabaseSettings settings) {
        try {
            prepareDatabaseConnection(settings);
            navigator.showLogin();
        } catch (Exception ex) {
            log.error("No se pudo inicializar PostgreSQL", ex);
            if (configStore.exists()) {
                pendingConnectionError = ex.getMessage();
                navigator.showLogin();
            } else {
                navigator.showDbSetup(ex.getMessage());
            }
        }
    }

    public void reconnectFromAdmin(DatabaseSettings settings) {
        prepareDatabaseConnection(settings);
    }

    public void prepareDatabaseConnection(DatabaseSettings settings) {
        shutdownPersistence();
        configStore.save(settings);
        initializePersistence(settings);
    }

    public void openModule(String id) {
        if (shellController != null) {
            shellController.openById(id);
        }
    }

    public void registerShell(ShellController controller) {
        this.shellController = controller;
    }

    public void logout() {
        sessionContext.clear();
        shellController = null;
        navigator.showLogin();
    }

    public void openDatabaseSetup() {
        authorizationService.require(sessionContext.requireUser(), PermissionCode.SETTINGS_MANAGE);
        openModule("settings");
    }

    public void openDemoAccounts() {
        navigator.showDemoAccounts();
    }

    public void returnToLogin() {
        if (sessionContext.isAuthenticated()) {
            return;
        }
        navigator.showLogin();
    }

    public void returnToLoginWith(String username, String password) {
        pendingLogin = new PendingLoginFill(username, password);
        returnToLogin();
    }

    public Optional<PendingLoginFill> consumePendingLogin() {
        PendingLoginFill fill = pendingLogin;
        pendingLogin = null;
        return Optional.ofNullable(fill);
    }

    public Optional<String> consumePendingConnectionError() {
        String message = pendingConnectionError;
        pendingConnectionError = null;
        return Optional.ofNullable(message);
    }

    public boolean canReturnToLogin() {
        return configStore.exists();
    }

    public boolean isDatabaseReady() {
        return authService != null;
    }

    public Optional<AuthService> authService() {
        return Optional.ofNullable(authService);
    }

    public void ensureDatabaseReady(DatabaseSettings settings) {
        if (authService == null) {
            prepareDatabaseConnection(settings);
        }
    }

    public DbConnectionSnapshot connectionSnapshot() {
        return connectionSnapshot;
    }

    public void saveConnectionSnapshot(DbConnectionSnapshot snapshot) {
        this.connectionSnapshot = snapshot == null ? DbConnectionSnapshot.unknown() : snapshot;
    }

    public void closeStage() {
        stage.close();
    }

    public Stage stage() {
        return stage;
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

    public DatabaseBootstrap databaseBootstrap() {
        return databaseBootstrap;
    }

    Object createController(Class<?> type) {
        if (type == DemoAccountsController.class) {
            return new DemoAccountsController(this);
        }
        if (type == DbSetupController.class) {
            return new DbSetupController(this, configStore, databaseBootstrap, properties);
        }
        if (type == LoginController.class) {
            return new LoginController(authService, sessionContext, navigator, this, configStore, databaseBootstrap);
        }
        if (type == ShellController.class) {
            return new ShellController(sessionContext, authorizationService, navigator, this);
        }
        if (type == DashboardController.class) {
            return new DashboardController(sessionContext, clientQueryService, this);
        }
        if (type == ClientsController.class) {
            return new ClientsController(clientService, sessionContext, authorizationService);
        }
        if (type == MembershipsController.class) {
            return new MembershipsController(membershipService, sessionContext, authorizationService);
        }
        if (type == PaymentsController.class) {
            return new PaymentsController(paymentService, sessionContext, authorizationService);
        }
        if (type == PlaceholderController.class) {
            return new PlaceholderController();
        }
        throw new IllegalArgumentException("No hay factory para " + type.getName());
    }

    private void initializePersistence(DatabaseSettings settings) {
        databaseBootstrap.ensureDatabaseExists(settings);
        flywayMigrator.migrate(settings);
        boolean showSql = Boolean.parseBoolean(properties.get("hibernate.show_sql", "false"));
        persistenceManager = new PersistenceManager(settings, showSql);
        UserRepository userRepository = new UserRepository(persistenceManager);
        ClientRepository clientRepository = new ClientRepository(persistenceManager);
        AccessCredentialRepository credentialRepository = new AccessCredentialRepository(persistenceManager);
        MembershipPlanRepository membershipPlanRepository = new MembershipPlanRepository(persistenceManager);
        ClientMembershipRepository clientMembershipRepository = new ClientMembershipRepository(persistenceManager);
        PaymentRepository paymentRepository = new PaymentRepository(persistenceManager);
        authService = new AuthService(userRepository, passwordHasher);
        clientQueryService = new ClientQueryService(clientRepository, credentialRepository);
        membershipService = new MembershipService(
                membershipPlanRepository,
                clientMembershipRepository,
                clientRepository,
                credentialRepository,
                Clock.systemDefaultZone());
        paymentService = new PaymentService(
                paymentRepository,
                clientRepository,
                clientMembershipRepository,
                credentialRepository,
                Clock.systemDefaultZone());
        clientService = new ClientService(
                clientRepository,
                credentialRepository,
                membershipService,
                Clock.systemDefaultZone());
        new DevDataSeeder(userRepository, passwordHasher).seedIfEmpty();
        new ClientDemoSeeder(clientRepository, clientService).seedIfEmpty();
        new MembershipDemoSeeder(clientRepository, clientMembershipRepository, membershipService)
                .seedMissingForActiveClients();
        new PaymentDemoSeeder(clientRepository, clientMembershipRepository, paymentRepository, paymentService)
                .seedIfEmpty();
    }

    private void shutdownPersistence() {
        if (persistenceManager != null) {
            persistenceManager.close();
            persistenceManager = null;
        }
    }
}
