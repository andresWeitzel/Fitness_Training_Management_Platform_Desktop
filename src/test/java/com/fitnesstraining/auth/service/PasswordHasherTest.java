package com.fitnesstraining.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPassword() {
        String hash = hasher.hash("1234");
        assertNotEquals("1234", hash);
        assertTrue(hasher.matches("1234", hash));
        assertFalse(hasher.matches("wrong", hash));
    }

    @Test
    void rejectsNulls() {
        assertFalse(hasher.matches(null, "x"));
        assertFalse(hasher.matches("x", null));
    }
}
