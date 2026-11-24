package com.gymcrm.workload.config;

import com.gymcrm.workload.repository.TrainerWorkloadDocument;
import jakarta.validation.ConstraintViolationException;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("MongoConfig")
class MongoConfigTest {

    private final MongoConfig config = new MongoConfig();

    private LocalValidatorFactoryBean newValidatorFactory() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        return validatorFactory;
    }

    @Test
    @DisplayName("validatingMongoEventListener builds a listener wired to the given validator")
    void validatingMongoEventListener_isCreated() {
        ValidatingMongoEventListener listener = config.validatingMongoEventListener(newValidatorFactory());

        assertNotNull(listener);
    }

    @Test
    @DisplayName("rejects a document that violates its declared constraints before it is saved")
    void rejectsInvalidDocument() {
        ValidatingMongoEventListener listener = config.validatingMongoEventListener(newValidatorFactory());

        TrainerWorkloadDocument blankUsername =
                new TrainerWorkloadDocument("", "Bob", "Jones", true, List.of(), 0L);
        BeforeSaveEvent<Object> event =
                new BeforeSaveEvent<>(blankUsername, new Document(), "trainer_workloads");

        assertThrows(ConstraintViolationException.class, () -> listener.onBeforeSave(event));
    }

    @Test
    @DisplayName("accepts a document that satisfies its declared constraints")
    void acceptsValidDocument() {
        ValidatingMongoEventListener listener = config.validatingMongoEventListener(newValidatorFactory());

        TrainerWorkloadDocument valid =
                new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, List.of(), 0L);
        BeforeSaveEvent<Object> event =
                new BeforeSaveEvent<>(valid, new Document(), "trainer_workloads");

        assertDoesNotThrow(() -> listener.onBeforeSave(event));
    }
}
