package com.fitnesstraining.auth.service;

import com.fitnesstraining.auth.dto.AuthenticatedUser;
import com.fitnesstraining.auth.model.Permission;
import com.fitnesstraining.auth.model.PermissionCode;
import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.shared.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordHasher passwordHasher = new PasswordHasher();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordHasher);
    }

    @Test
    void rejectsBlankCredentialsWithoutHittingRepository() {
        assertThrows(AuthenticationException.class, () -> authService.login("  ", "1234"));
        verify(userRepository, never()).findActiveByUsername(any());
    }

    @Test
    void hidesWhetherUserExists() {
        when(userRepository.findActiveByUsername("admin")).thenReturn(Optional.empty());
        AuthenticationException ex = assertThrows(
                AuthenticationException.class,
                () -> authService.login("admin", "1234")
        );
        assertEquals(AuthService.INVALID_CREDENTIALS, ex.getMessage());
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findActiveByUsername("admin")).thenReturn(Optional.of(user("admin", "1234")));
        assertThrows(AuthenticationException.class, () -> authService.login("admin", "otra"));
        verify(userRepository, never()).updateLastLogin(any(), any());
    }

    @Test
    void returnsAuthenticatedUserAndRecordsLastLogin() {
        User user = user("admin", "1234");
        when(userRepository.findActiveByUsername("admin")).thenReturn(Optional.of(user));

        AuthenticatedUser authenticated = authService.login("admin", "1234");

        assertEquals("admin", authenticated.username());
        assertTrue(authenticated.hasPermission(PermissionCode.DASHBOARD_VIEW));
        assertTrue(authenticated.roles().contains(RoleName.ADMIN.name()));
        verify(userRepository).updateLastLogin(eq(user.getId()), any());
    }

    private User user(String username, String rawPassword) {
        Role role = new Role(RoleName.ADMIN.name(), "Administrador");
        role.getPermissions().add(new Permission(PermissionCode.DASHBOARD_VIEW.name(), "Dashboard"));
        User user = new User(username, passwordHasher.hash(rawPassword), "Administrador", "admin@local");
        user.addRole(role);
        return user;
    }
}
