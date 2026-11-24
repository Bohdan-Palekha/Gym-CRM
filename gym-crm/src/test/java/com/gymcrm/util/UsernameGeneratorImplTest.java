package com.gymcrm.util;

import com.gymcrm.util.impl.UsernameGeneratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UsernameGeneratorImpl — naming algorithm")
class UsernameGeneratorImplTest {

    private UsernameGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UsernameGeneratorImpl();
    }

    @Nested
    @DisplayName("No collision")
    class NoCollision {

        @Test
        @DisplayName("returns firstName.lastName when existing set is empty")
        void emptySet_returnsBase() {
            assertEquals("John.Smith", generator.generate("John", "Smith", Set.of()));
        }

        @Test
        @DisplayName("returns base when only unrelated usernames exist")
        void unrelatedNames_returnsBase() {
            assertEquals("John.Smith",
                    generator.generate("John", "Smith", Set.of("Jane.Doe", "Bob.Brown")));
        }

        @Test
        @DisplayName("accepts empty Collection (List) as existing set")
        void emptyList_returnsBase() {
            assertEquals("Alice.Brown", generator.generate("Alice", "Brown", List.of()));
        }
    }

    @Nested
    @DisplayName("Collision handling")
    class CollisionHandling {

        @Test
        @DisplayName("one collision → appends suffix 1")
        void oneCollision_suffix1() {
            assertEquals("John.Smith1",
                    generator.generate("John", "Smith", Set.of("John.Smith")));
        }

        @Test
        @DisplayName("two collisions → appends suffix 2")
        void twoCollisions_suffix2() {
            assertEquals("John.Smith2",
                    generator.generate("John", "Smith",
                            Set.of("John.Smith", "John.Smith1")));
        }

        @Test
        @DisplayName("five collisions → appends suffix 5")
        void fiveCollisions_suffix5() {
            assertEquals("John.Smith5",
                    generator.generate("John", "Smith",
                            Set.of("John.Smith", "John.Smith1", "John.Smith2",
                                    "John.Smith3", "John.Smith4")));
        }
    }

    @Nested
    @DisplayName("Input trimming")
    class InputTrimming {

        @Test
        @DisplayName("leading and trailing whitespace is trimmed from both names")
        void whitespace_trimmed() {
            assertEquals("John.Smith",
                    generator.generate("  John  ", "  Smith  ", Set.of()));
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("null firstName throws IllegalArgumentException")
        void nullFirstName_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate(null, "Smith", Set.of()));
        }

        @Test
        @DisplayName("blank firstName throws IllegalArgumentException")
        void blankFirstName_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate("   ", "Smith", Set.of()));
        }

        @Test
        @DisplayName("null lastName throws IllegalArgumentException")
        void nullLastName_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate("John", null, Set.of()));
        }

        @Test
        @DisplayName("empty lastName throws IllegalArgumentException")
        void emptyLastName_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate("John", "", Set.of()));
        }
    }

    @Nested
    @DisplayName("Separator")
    class Separator {

        @Test
        @DisplayName("separator is a dot character")
        void usesDotSeparator() {
            String result = generator.generate("John", "Smith", Set.of());
            assertTrue(result.contains("."));
            assertFalse(result.contains("_"));
            assertFalse(result.contains(" "));
        }
    }
}
