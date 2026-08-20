package com.fitnesstraining.auth.service;

import com.fitnesstraining.auth.model.Role;
import com.fitnesstraining.auth.model.RoleName;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoCredentialStoreTest {

    @TempDir Path tempDir;

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;

    @Test
    void buildPanelShowsAllFourRolesFromDatabase() {
        User admin = user(1L, "admin", "hash-a", "ADMIN");
        User reception = user(2L, "empleado1", "hash-e", "RECEPTIONIST");
        User trainer = user(3L, "juan_prof", "hash-t", "TRAINER");
        User nutri = user(4L, "maria_nutri", "hash-n", "NUTRITIONIST");
        when(userRepository.listActive()).thenReturn(List.of(admin, reception, trainer, nutri));
        when(passwordHasher.matches("1234", "hash-a")).thenReturn(true);
        when(passwordHasher.matches("emp123", "hash-e")).thenReturn(true);
        when(passwordHasher.matches("prof123", "hash-t")).thenReturn(true);
        when(passwordHasher.matches("nutri123", "hash-n")).thenReturn(true);

        DemoCredentialStore store = new DemoCredentialStore(tempDir.resolve("demo.properties"));
        List<DemoCredentialStore.DemoAccount> rows = store.buildPanelAccounts(userRepository, passwordHasher);

        assertEquals(4, rows.size());
        assertEquals(RoleName.RECEPTIONIST, rows.get(1).role());
        assertTrue(rows.get(1).passwordKnown());
    }

    @Test
    void renamedSeedUserStillResolvesDefaultPassword() {
        User renamed = user(2L, "empleado01", "hash-e", "RECEPTIONIST");
        when(userRepository.listActive()).thenReturn(List.of(renamed));
        when(passwordHasher.matches(org.mockito.ArgumentMatchers.anyString(), eq("hash-e")))
                .thenAnswer(inv -> "emp123".equals(inv.getArgument(0)));

        DemoCredentialStore store = new DemoCredentialStore(tempDir.resolve("demo.properties"));
        List<DemoCredentialStore.DemoAccount> rows = store.buildPanelAccounts(userRepository, passwordHasher);

        assertEquals(4, rows.size());
        DemoCredentialStore.DemoAccount reception = rows.stream()
                .filter(r -> r.role() == RoleName.RECEPTIONIST)
                .findFirst()
                .orElseThrow();
        assertEquals("empleado01", reception.username());
        assertEquals("emp123", reception.password());
        assertTrue(reception.passwordKnown());
        assertFalse(DemoCredentialStore.labelFor(reception).contains("clave no disponible"));
    }

    @Test
    void rememberedPasswordSurvivesReload() {
        Path file = tempDir.resolve("demo.properties");
        DemoCredentialStore store = new DemoCredentialStore(file);
        store.rememberPassword(2L, "miClaveNueva");

        User renamed = user(2L, "empleado01", "hash-custom", "RECEPTIONIST");
        when(userRepository.listActive()).thenReturn(List.of(renamed));
        when(passwordHasher.matches("miClaveNueva", "hash-custom")).thenReturn(true);

        DemoCredentialStore reloaded = new DemoCredentialStore(file);
        List<DemoCredentialStore.DemoAccount> rows = reloaded.buildPanelAccounts(userRepository, passwordHasher);
        DemoCredentialStore.DemoAccount reception = rows.stream()
                .filter(r -> r.role() == RoleName.RECEPTIONIST)
                .findFirst()
                .orElseThrow();
        assertEquals("miClaveNueva", reception.password());
    }

    @Test
    void roleChangeMovesUserToNewRoleSlot() {
        User admin = user(1L, "admin", "hash-a", "ADMIN");
        User nowTrainer = user(2L, "empleado1", "hash-e", "TRAINER");
        User trainer = user(3L, "juan_prof", "hash-t", "TRAINER");
        User nutri = user(4L, "maria_nutri", "hash-n", "NUTRITIONIST");
        when(userRepository.listActive()).thenReturn(List.of(admin, nowTrainer, trainer, nutri));
        when(passwordHasher.matches("1234", "hash-a")).thenReturn(true);
        when(passwordHasher.matches("emp123", "hash-e")).thenReturn(true);
        when(passwordHasher.matches("prof123", "hash-t")).thenReturn(true);
        when(passwordHasher.matches("nutri123", "hash-n")).thenReturn(true);

        DemoCredentialStore store = new DemoCredentialStore(tempDir.resolve("demo.properties"));
        List<DemoCredentialStore.DemoAccount> rows = store.buildPanelAccounts(userRepository, passwordHasher);

        assertEquals(5, rows.size());
        DemoCredentialStore.DemoAccount emptyReception = rows.stream()
                .filter(r -> r.role() == RoleName.RECEPTIONIST)
                .findFirst()
                .orElseThrow();
        assertFalse(emptyReception.active());
    }

    private static User user(Long id, String username, String hash, String roleName) {
        User user = withId(new User(username, hash, username, null), id);
        user.addRole(new Role(roleName, roleName));
        return user;
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
