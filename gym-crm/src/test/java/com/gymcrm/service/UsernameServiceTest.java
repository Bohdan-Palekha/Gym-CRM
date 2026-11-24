package com.gymcrm.service;

import com.gymcrm.util.UsernameGenerator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsernameService")
class UsernameServiceTest {

    @Mock
    private UsernameGenerator usernameGenerator;

    private UsernameService service;

    @BeforeEach
    void setUp() {
        service = new UsernameService();
        service.setUsernameGenerator(usernameGenerator);
    }

    @Nested
    @DisplayName("validateName")
    class ValidateName {

        @Test
        @DisplayName("valid name passes without exception")
        void valid() {
            assertDoesNotThrow(() -> service.validateName("John", "firstName"));
        }

        @Test
        @DisplayName("null throws IllegalArgumentException")
        void nullValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.validateName(null, "firstName"));
        }

        @Test
        @DisplayName("blank throws IllegalArgumentException")
        void blankValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.validateName("  ", "lastName"));
        }
    }

    @Nested
    @DisplayName("reserveUsername")
    class ReserveUsername {

        @Test
        @DisplayName("delegates to UsernameGenerator, then claims via internal registry")
        void delegatesToGenerator() {
            when(usernameGenerator.generate(eq("John"), eq("Smith"), any()))
                    .thenReturn("John.Smith");

            String result = service.reserveUsername("John", "Smith");

            assertEquals("John.Smith", result);
            verify(usernameGenerator).generate(eq("John"), eq("Smith"), any());
        }

        @Test
        @DisplayName("retries when generator returns an already-claimed candidate")
        void retriesOnCollision() {
            when(usernameGenerator.generate(eq("John"), eq("Smith"), any()))
                    .thenReturn("John.Smith")     // first call claims it
                    .thenReturn("John.Smith")     // second call collides — must retry
                    .thenReturn("John.Smith1");   // free

            service.reserveUsername("John", "Smith");                 // claims "John.Smith"
            String result = service.reserveUsername("John", "Smith"); // forces retry

            assertEquals("John.Smith1", result);
            verify(usernameGenerator, times(3)).generate(eq("John"), eq("Smith"), any());
        }

        @Test
        @DisplayName("same candidate cannot be reserved twice")
        void preventsExactDuplicateAcrossCalls() {
            when(usernameGenerator.generate(any(), any(), any()))
                    .thenReturn("Alice.Green")
                    .thenReturn("Alice.Green1");

            String first = service.reserveUsername("Alice", "Green");
            String second = service.reserveUsername("Alice", "Green");

            assertNotEquals(first, second);
        }
    }

    @Nested
    @DisplayName("seedRegistryFromDb")
    class SeedRegistryFromDb {

        @Mock
        private SessionFactory sessionFactory;
        @Mock
        private Session session;
        @Mock
        private Query<String> query;

        @Test
        @DisplayName("loads existing usernames so subsequent generation avoids them")
        @SuppressWarnings("unchecked")
        void seeds_thenBlocksCollision() {
            service.setSessionFactory(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.createQuery(anyString(), eq(String.class))).thenReturn(query);
            when(query.list()).thenReturn(List.of("Existing.User"));

            service.seedRegistryFromDb();

            when(usernameGenerator.generate(any(), any(), any()))
                    .thenReturn("Existing.User")
                    .thenReturn("Existing.User1");

            String result = service.reserveUsername("Existing", "User");
            assertEquals("Existing.User1", result);
        }

        @Test
        @DisplayName("does not throw when sessionFactory is null (pure unit-test context)")
        void nullSessionFactory_doesNotThrow() {
            assertDoesNotThrow(() -> service.seedRegistryFromDb());
        }

        @Test
        @DisplayName("does not propagate exceptions from a failing DB query (first-run safety)")
        void dbFailure_doesNotPropagate() {
            service.setSessionFactory(sessionFactory);
            when(sessionFactory.openSession()).thenThrow(new RuntimeException("table missing"));
            assertDoesNotThrow(() -> service.seedRegistryFromDb());
        }
    }
}
