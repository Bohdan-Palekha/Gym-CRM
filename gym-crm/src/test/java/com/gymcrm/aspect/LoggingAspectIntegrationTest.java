package com.gymcrm.aspect;

import com.gymcrm.config.AppConfig;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {AppConfig.class, LoggingAspectIntegrationTest.TestMetricsConfig.class})
@TestPropertySource(locations = "classpath:application.properties")
@DisplayName("LoggingAspect — AOP proxy-presence integration tests")
class LoggingAspectIntegrationTest {

    @Configuration
    static class TestMetricsConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired private TraineeService traineeService;
    @Autowired private TrainerService trainerService;
    @Autowired private TrainingService trainingService;

    @Test
    @DisplayName("TraineeService is a Spring AOP proxy — Pointcuts.SERVICE is active")
    void traineeService_isProxy() {
        assertTrue(AopUtils.isAopProxy(traineeService),
                "TraineeService must be an AOP proxy. " +
                "Failure means Pointcuts.SERVICE does not match the service package.");
    }

    @Test
    @DisplayName("TrainerService is a Spring AOP proxy")
    void trainerService_isProxy() {
        assertTrue(AopUtils.isAopProxy(trainerService),
                "TrainerService must be an AOP proxy.");
    }

    @Test
    @DisplayName("TrainingService is a Spring AOP proxy")
    void trainingService_isProxy() {
        assertTrue(AopUtils.isAopProxy(trainingService),
                "TrainingService must be an AOP proxy.");
    }

    @Test
    @DisplayName("Calling a proxied service method does not throw proxy-wiring exceptions")
    void proxiedMethod_doesNotThrowWiringException() {
        assertDoesNotThrow(() -> traineeService.selectAllTrainees(),
                "Calling selectAllTrainees() through the proxy must not throw");
    }
}
