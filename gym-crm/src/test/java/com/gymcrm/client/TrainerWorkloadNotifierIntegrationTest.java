package com.gymcrm.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.client.dto.ActionType;
import com.gymcrm.client.dto.TrainerWorkloadRequest;
import com.gymcrm.client.security.ServiceAuthTokenMessagePostProcessor;
import com.gymcrm.client.security.TransactionIdMessagePostProcessor;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("TrainerWorkloadNotifierImpl - real Spring context + embedded broker")
class TrainerWorkloadNotifierIntegrationTest {

    @Autowired
    private TrainerWorkloadNotifierImpl notifier;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${trainer-workload.jms.destination.workload-updates}")
    private String destination;

    private JmsTemplate receiveTemplate;

    @BeforeEach
    void setUp() {
        receiveTemplate = new JmsTemplate(jmsTemplate.getConnectionFactory());
        receiveTemplate.setReceiveTimeout(3000);
        MDC.clear();
        drainQueue();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        drainQueue();
    }

    private void drainQueue() {
        JmsTemplate noWaitTemplate = new JmsTemplate(jmsTemplate.getConnectionFactory());
        noWaitTemplate.setReceiveTimeout(50);

        while (noWaitTemplate.receive(destination) != null) {
            // drain
        }
    }

    @Test
    @DisplayName("trainingAdded publishes a JSON message carrying the auth token and transaction id properties")
    void trainingAdded_publishesRealJmsMessage() throws JMSException, JsonProcessingException {
        MDC.put("transactionId", "txn-integration-1");

        Training training =
                trainingFor("jane.doe", "Jane", "Doe", LocalDate.of(2026, 4, 15), 45);

        notifier.trainingAdded(training);

        Message received = receiveTemplate.receive(destination);

        assertNotNull(received, "expected a message to actually be published to the broker");
        assertTrue(received instanceof TextMessage, "expected a TextMessage carrying JSON");

        TrainerWorkloadRequest request =
                objectMapper.readValue(((TextMessage) received).getText(), TrainerWorkloadRequest.class);

        assertEquals("jane.doe", request.trainerUsername());
        assertEquals("Jane", request.trainerFirstName());
        assertEquals(ActionType.ADD, request.actionType());
        assertEquals(45, request.trainingDuration());

        String authToken = received.getStringProperty(
                ServiceAuthTokenMessagePostProcessor.SERVICE_AUTH_TOKEN_PROPERTY);

        assertNotNull(authToken, "expected a service auth token property on the message");
        assertEquals(3, authToken.split("\\.").length);

        assertEquals(
                "txn-integration-1",
                received.getStringProperty(
                        TransactionIdMessagePostProcessor.TRANSACTION_ID_PROPERTY));
    }

    @Test
    @DisplayName("trainingDeleted publishes ActionType.DELETE")
    void trainingDeleted_publishesRealJmsMessage() throws JMSException, JsonProcessingException {
        Training training =
                trainingFor("mike.jones", "Mike", "Jones", LocalDate.of(2026, 5, 1), 30);

        notifier.trainingDeleted(training);

        Message received = receiveTemplate.receive(destination);

        assertNotNull(received);
        assertTrue(received instanceof TextMessage);

        TrainerWorkloadRequest request =
                objectMapper.readValue(((TextMessage) received).getText(), TrainerWorkloadRequest.class);

        assertEquals(ActionType.DELETE, request.actionType());
    }

    private Training trainingFor(
            String username,
            String firstName,
            String lastName,
            LocalDate date,
            int minutes) {

        Trainer trainer = new Trainer();
        trainer.setUsername(username);
        trainer.setFirstName(firstName);
        trainer.setLastName(lastName);
        trainer.setActive(true);

        Training training = new Training();
        training.setTrainer(trainer);
        training.setTrainingDate(date);
        training.setDurationMinutes(minutes);
        return training;
    }
}