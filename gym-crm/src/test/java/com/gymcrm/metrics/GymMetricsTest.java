package com.gymcrm.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GymMetrics")
class GymMetricsTest {

    private SimpleMeterRegistry registry;
    private GymMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new GymMetrics(registry);
    }

    @Test
    @DisplayName("constructor pre-registers every counter at zero so they are visible immediately")
    void countersVisibleImmediatelyAfterConstruction() {
        assertEquals(0.0, registry.counter("gym.registrations", "role", "trainee").count());
        assertEquals(0.0, registry.counter("gym.registrations", "role", "trainer").count());
        assertEquals(0.0, registry.counter("gym.authentication.attempts",
                "role", "trainee", "result", "success").count());
        assertEquals(0.0, registry.counter("gym.authentication.attempts",
                "role", "trainer", "result", "failure").count());
        assertEquals(0.0, registry.counter("gym.authentication.attempts",
                "role", "unknown", "result", "failure").count());
    }

    @Test
    @DisplayName("trainee registrations increment only the trainee-tagged counter")
    void traineeRegistration_incrementsCorrectCounter() {
        metrics.incrementTraineeRegistrations();
        metrics.incrementTraineeRegistrations();

        assertEquals(2.0, registry.counter("gym.registrations", "role", "trainee").count());
        assertEquals(0.0, registry.counter("gym.registrations", "role", "trainer").count());
    }

    @Test
    @DisplayName("trainer registrations increment only the trainer-tagged counter")
    void trainerRegistration_incrementsCorrectCounter() {
        metrics.incrementTrainerRegistrations();

        assertEquals(1.0, registry.counter("gym.registrations", "role", "trainer").count());
        assertEquals(0.0, registry.counter("gym.registrations", "role", "trainee").count());
    }

    @Test
    @DisplayName("authentication outcomes increment only the matching role/result counter")
    void authOutcomes_incrementMatchingCountersOnly() {
        metrics.recordTraineeAuthSuccess();
        metrics.recordTraineeAuthFailure();
        metrics.recordTraineeAuthFailure();
        metrics.recordTrainerAuthSuccess();

        assertEquals(1.0, registry.counter("gym.authentication.attempts",
                "role", "trainee", "result", "success").count());
        assertEquals(2.0, registry.counter("gym.authentication.attempts",
                "role", "trainee", "result", "failure").count());
        assertEquals(1.0, registry.counter("gym.authentication.attempts",
                "role", "trainer", "result", "success").count());
        assertEquals(0.0, registry.counter("gym.authentication.attempts",
                "role", "trainer", "result", "failure").count());
    }

    @Test
    @DisplayName("unknown-user failures increment only the role=unknown counter, not trainee or trainer")
    void unknownUserFailure_incrementsOnlyUnknownCounter() {
        metrics.recordUnknownUserAuthFailure();

        assertEquals(1.0, registry.counter("gym.authentication.attempts",
                "role", "unknown", "result", "failure").count());
        assertEquals(0.0, registry.counter("gym.authentication.attempts",
                "role", "trainee", "result", "failure").count());
        assertEquals(0.0, registry.counter("gym.authentication.attempts",
                "role", "trainer", "result", "failure").count());
    }
}
