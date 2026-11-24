package com.gymcrm.util;

import com.gymcrm.util.impl.PasswordGeneratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordGeneratorImpl")
class PasswordGeneratorImplTest {

    private static final String ALLOWED_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private PasswordGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PasswordGeneratorImpl();
    }

    @Test
    @DisplayName("generated password is exactly 10 characters")
    void generate_returnsExactly10Chars() {
        assertEquals(10, generator.generate().length());
    }

    @RepeatedTest(50)
    @DisplayName("password contains only alphanumeric characters (repeated 50x)")
    void generate_containsOnlyAllowedChars() {
        String password = generator.generate();
        for (char c : password.toCharArray()) {
            assertTrue(ALLOWED_CHARS.indexOf(c) >= 0,
                    "Unexpected character '" + c + "' in: " + password);
        }
    }

    @Test
    @DisplayName("generated password is not null or blank")
    void generate_notNullOrBlank() {
        String password = generator.generate();
        assertNotNull(password);
        assertFalse(password.isBlank());
    }

    @Test
    @DisplayName("1000 generated passwords are all unique (statistical uniqueness)")
    void generate_highUniqueness() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            passwords.add(generator.generate());
        }
        assertEquals(1000, passwords.size(),
                "Expected 1000 unique passwords but found duplicates");
    }

    @Test
    @DisplayName("consecutive calls return different passwords")
    void generate_consecutiveCalls_differentResults() {
        String p1 = generator.generate();
        String p2 = generator.generate();
        assertNotEquals(p1, p2,
                "Two consecutive passwords should not be equal (RNG check)");
    }
}
