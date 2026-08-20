package com.fitnesstraining.staff.service;

import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.RoleRepository;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.auth.service.DemoCredentialStore;
import com.fitnesstraining.auth.service.PasswordHasher;
import com.fitnesstraining.shared.exception.AppException;
import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.staff.dto.StaffRequest;
import com.fitnesstraining.staff.dto.StaffRoleOption;
import com.fitnesstraining.staff.dto.StaffSummary;
import com.fitnesstraining.staff.dto.StaffView;
import com.fitnesstraining.staff.model.StaffListScope;
import com.fitnesstraining.staff.validation.StaffValidator;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final DemoCredentialStore demoCredentialStore;
    private final Clock clock;

    public StaffService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher,
            DemoCredentialStore demoCredentialStore,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.demoCredentialStore = demoCredentialStore;
        this.clock = clock;
    }

    public List<StaffRoleOption> listRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new StaffRoleOption(
                        RoleName.valueOf(role.getName()),
                        labelForRole(role.getName()),
                        role.getDescription()))
                .toList();
    }

    public List<StaffSummary> list(String term, StaffListScope scope) {
        List<User> users;
        if (term == null || term.isBlank()) {
            users = switch (scope) {
                case ACTIVE -> userRepository.listActive();
                case INACTIVE -> userRepository.listInactive();
                case ALL -> userRepository.listAll();
            };
        } else {
            Boolean activeOnly = switch (scope) {
                case ACTIVE -> Boolean.TRUE;
                case INACTIVE -> Boolean.FALSE;
                case ALL -> null;
            };
            users = userRepository.search(term.trim(), activeOnly);
        }
        return users.stream().map(this::toSummary).toList();
    }

    public StaffView get(Long id) {
        return toView(requireUser(id));
    }

    public StaffView create(StaffRequest request) {
        StaffRequest normalized = StaffValidator.normalizeAndValidateCreate(request);
        if (userRepository.existsUsername(normalized.username(), null)) {
            throw new ValidationException("Ya existe un usuario con ese nombre.");
        }
        Role role = requireRoleEntity(normalized.role());
        User user = new User(
                normalized.username().toLowerCase(Locale.ROOT),
                passwordHasher.hash(normalized.password()),
                normalized.displayName(),
                normalized.email());
        userRepository.createWithRole(user, role.getName());
        StaffView view = toView(requireUser(user.getId()));
        demoCredentialStore.rememberPassword(view.id(), normalized.password());
        return view;
    }

    public StaffView update(Long id, StaffRequest request) {
        User user = requireUser(id);
        if (!user.isActive()) {
            throw new ValidationException("Reactive el usuario antes de editarlo.");
        }
        StaffRequest normalized = StaffValidator.normalizeAndValidateUpdate(request);
        OffsetDateTime now = now();

        String nextUsername = normalized.username().toLowerCase(Locale.ROOT);
        if (userRepository.existsUsername(nextUsername, id)) {
            throw new ValidationException("Ya existe un usuario con ese nombre.");
        }

        RoleName currentRole = primaryRole(user);
        RoleName nextRole = normalized.role();
        if (currentRole == RoleName.ADMIN
                && nextRole != RoleName.ADMIN
                && userRepository.countActiveAdmins() <= 1) {
            throw new ValidationException("No se puede quitar el rol Admin del último administrador activo.");
        }

        String passwordHash = normalized.password() == null
                ? null
                : passwordHasher.hash(normalized.password());
        userRepository.updateStaff(
                id,
                nextUsername,
                normalized.displayName(),
                normalized.email(),
                passwordHash,
                nextRole.name(),
                now);
        StaffView view = toView(requireUser(id));
        if (normalized.password() != null) {
            demoCredentialStore.rememberPassword(view.id(), normalized.password());
        } else {
            demoCredentialStore.syncProfile(
                    view.id(),
                    view.username(),
                    view.role(),
                    view.displayName(),
                    view.active());
        }
        return view;
    }

    public StaffView deactivate(Long id, Long actingUserId) {
        User user = requireUser(id);
        if (!user.isActive()) {
            throw new ValidationException("El usuario ya está dado de baja.");
        }
        if (actingUserId != null && actingUserId.equals(id)) {
            throw new ValidationException("No puede darse de baja a sí mismo.");
        }
        if (primaryRole(user) == RoleName.ADMIN && userRepository.countActiveAdmins() <= 1) {
            throw new ValidationException("No se puede dar de baja al último administrador activo.");
        }
        user.deactivate(now());
        userRepository.save(user);
        return toView(requireUser(id));
    }

    public StaffView reactivate(Long id) {
        User user = requireUser(id);
        if (user.isActive()) {
            throw new ValidationException("El usuario ya está activo.");
        }
        user.reactivate(now());
        userRepository.save(user);
        return toView(requireUser(id));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("Usuario no encontrado."));
    }

    private Role requireRoleEntity(RoleName roleName) {
        return roleRepository.findByName(roleName.name())
                .orElseThrow(() -> new ValidationException("Rol no encontrado."));
    }

    private StaffSummary toSummary(User user) {
        RoleName role = primaryRole(user);
        return new StaffSummary(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                role,
                labelForRole(role == null ? null : role.name()),
                user.isActive());
    }

    private StaffView toView(User user) {
        RoleName role = primaryRole(user);
        return new StaffView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                role,
                labelForRole(role == null ? null : role.name()),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    private static RoleName primaryRole(User user) {
        String name = user.primaryRoleName();
        if (name == null) {
            return null;
        }
        return EnumSet.allOf(RoleName.class).stream()
                .filter(role -> role.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static String labelForRole(String roleName) {
        if (roleName == null) {
            return "Sin rol";
        }
        return switch (roleName) {
            case "ADMIN" -> "Administrador";
            case "RECEPTIONIST" -> "Recepción";
            case "TRAINER" -> "Entrenador";
            case "NUTRITIONIST" -> "Nutricionista";
            default -> roleName;
        };
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
