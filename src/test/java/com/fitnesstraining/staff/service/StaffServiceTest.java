package com.fitnesstraining.staff.service;

import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.RoleRepository;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.auth.service.DemoCredentialStore;
import com.fitnesstraining.auth.service.PasswordHasher;
import com.fitnesstraining.shared.exception.ValidationException;
import com.fitnesstraining.staff.dto.StaffRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T15:00:00Z"), ZoneOffset.UTC);

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordHasher passwordHasher;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(
                userRepository, roleRepository, passwordHasher, new DemoCredentialStore(null), CLOCK);
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsUsername("empleado2", null)).thenReturn(true);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                staffService.create(new StaffRequest(
                        "empleado2", "Empleado Dos", null, "pass", RoleName.RECEPTIONIST)));
        assertEquals("Ya existe un usuario con ese nombre.", ex.getMessage());
        verify(userRepository, never()).createWithRole(any(), anyString());
    }

    @Test
    void rejectsSelfDeactivate() {
        User user = withId(new User("admin", "hash", "Admin", null), 1L);
        user.addRole(new Role("ADMIN", "Administrador"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                staffService.deactivate(1L, 1L));
        assertEquals("No puede darse de baja a sí mismo.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDeactivateLastAdmin() {
        User user = withId(new User("admin", "hash", "Admin", null), 1L);
        user.addRole(new Role("ADMIN", "Administrador"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                staffService.deactivate(1L, 2L));
        assertEquals("No se puede dar de baja al último administrador activo.", ex.getMessage());
    }

    @Test
    void createsUserWithHashedPassword() {
        Role role = new Role("RECEPTIONIST", "Recepción");
        when(userRepository.existsUsername("empleado2", null)).thenReturn(false);
        when(roleRepository.findByName("RECEPTIONIST")).thenReturn(Optional.of(role));
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        doAnswer(inv -> {
            User user = inv.getArgument(0);
            withId(user, 10L);
            return null;
        }).when(userRepository).createWithRole(any(User.class), eq("RECEPTIONIST"));
        User saved = withId(new User("empleado2", "hashed", "Empleado Dos", null), 10L);
        saved.addRole(role);
        when(userRepository.findById(10L)).thenReturn(Optional.of(saved));

        var view = staffService.create(new StaffRequest(
                "empleado2", "Empleado Dos", null, "pass", RoleName.RECEPTIONIST));

        assertEquals("empleado2", view.username());
        assertEquals(RoleName.RECEPTIONIST, view.role());
        verify(passwordHasher).hash("pass");
    }

    @Test
    void updatesUsernameNameRoleAndPassword() {
        User existing = withId(new User("empleado2", "old-hash", "Viejo", null), 10L);
        existing.addRole(new Role("RECEPTIONIST", "Recepción"));
        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.existsUsername("empleado3", 10L)).thenReturn(false);
        when(passwordHasher.hash("nueva")).thenReturn("new-hash");
        when(userRepository.updateStaff(
                eq(10L),
                eq("empleado3"),
                eq("Empleado Tres"),
                eq("a@b.com"),
                eq("new-hash"),
                eq("TRAINER"),
                any())).thenReturn(existing);

        User refreshed = withId(new User("empleado3", "new-hash", "Empleado Tres", "a@b.com"), 10L);
        refreshed.addRole(new Role("TRAINER", "Entrenador"));
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(refreshed));

        var view = staffService.update(10L, new StaffRequest(
                "empleado3", "Empleado Tres", "a@b.com", "nueva", RoleName.TRAINER));

        assertEquals("empleado3", view.username());
        assertEquals("Empleado Tres", view.displayName());
        assertEquals(RoleName.TRAINER, view.role());
        verify(userRepository).updateStaff(
                eq(10L),
                eq("empleado3"),
                eq("Empleado Tres"),
                eq("a@b.com"),
                eq("new-hash"),
                eq("TRAINER"),
                any());
    }

    @Test
    void rejectsDuplicateUsernameOnUpdate() {
        User existing = withId(new User("empleado2", "hash", "Empleado", null), 10L);
        existing.addRole(new Role("RECEPTIONIST", "Recepción"));
        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.existsUsername("otro", 10L)).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                staffService.update(10L, new StaffRequest(
                        "otro", "Empleado", null, null, RoleName.RECEPTIONIST)));
        assertEquals("Ya existe un usuario con ese nombre.", ex.getMessage());
        verify(userRepository, never()).updateStaff(
                any(), anyString(), anyString(), any(), any(), anyString(), any());
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
