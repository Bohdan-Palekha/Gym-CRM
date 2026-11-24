package com.gymcrm.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

//business metrics recorded  with micrometer, scraped by prometheus
//a training-creation duration timer is declared with @Timed in TrainingService addTraining
//counter are precreated  in constructor
@Slf4j
@Component
public class GymMetrics {

    static final String REGISTRATIONS_METRIC = "gym.registrations";
    static final String AUTH_ATTEMPTS_METRIC = "gym.authentication.attempts";

    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter traineeAuthSuccess;
    private final Counter traineeAuthFailure;
    private final Counter trainerAuthSuccess;
    private final Counter trainerAuthFailure;
    private final Counter unknownUserAuthFailure;

    public GymMetrics(MeterRegistry registry) {
        this.traineeRegistrations = registry.counter(REGISTRATIONS_METRIC, "role", "trainee");
        this.trainerRegistrations = registry.counter(REGISTRATIONS_METRIC, "role", "trainer");
        this.traineeAuthSuccess = registry.counter(AUTH_ATTEMPTS_METRIC, "role", "trainee", "result", "success");
        this.traineeAuthFailure = registry.counter(AUTH_ATTEMPTS_METRIC, "role", "trainee", "result", "failure");
        this.trainerAuthSuccess = registry.counter(AUTH_ATTEMPTS_METRIC, "role", "trainer", "result", "success");
        this.trainerAuthFailure = registry.counter(AUTH_ATTEMPTS_METRIC, "role", "trainer", "result", "failure");
        this.unknownUserAuthFailure = registry.counter(AUTH_ATTEMPTS_METRIC, "role", "unknown", "result", "failure");
    }

    public void incrementTraineeRegistrations() {
        traineeRegistrations.increment();
        log.debug("Metric incremented: {} role=trainee", REGISTRATIONS_METRIC);
    }

    public void incrementTrainerRegistrations() {
        trainerRegistrations.increment();
        log.debug("Metric incremented: {} role=trainer", REGISTRATIONS_METRIC);
    }

    public void recordTraineeAuthSuccess() {
        traineeAuthSuccess.increment();
    }

    public void recordTraineeAuthFailure() {
        traineeAuthFailure.increment();
    }

    public void recordTrainerAuthSuccess() {
        trainerAuthSuccess.increment();
    }

    public void recordTrainerAuthFailure() {
        trainerAuthFailure.increment();
    }

    public void recordUnknownUserAuthFailure() {
        unknownUserAuthFailure.increment();
    }
}
