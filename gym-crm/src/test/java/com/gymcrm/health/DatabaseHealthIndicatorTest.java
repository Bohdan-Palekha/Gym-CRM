package com.gymcrm.health;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseHealthIndicator")
class DatabaseHealthIndicatorTest {

    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Session session;

    private DatabaseHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new DatabaseHealthIndicator(sessionFactory);
    }

    @Test
    @DisplayName("reports UP with a response time detail when the ping succeeds")
    void pingSucceeds_reportsUp() {
        when(sessionFactory.openSession()).thenReturn(session);

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("responseTimeMs"));
        verify(session).doWork(any(Work.class));
        verify(session).close();
    }

    @Test
    @DisplayName("reports DOWN with error detail when opening a session fails")
    void sessionOpenFails_reportsDown() {
        when(sessionFactory.openSession()).thenThrow(new RuntimeException("connection refused"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }

    @Test
    @DisplayName("reports DOWN when the ping query itself throws")
    void pingQueryFails_reportsDown() {
        when(sessionFactory.openSession()).thenReturn(session);
        doThrow(new RuntimeException("syntax error")).when(session).doWork(any(Work.class));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }
}
