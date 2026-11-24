package com.gymcrm.workload.messaging;

import com.gymcrm.workload.dto.ActionType;
import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.exception.TrainerWorkloadNotFoundException;
import com.gymcrm.workload.repository.InMemoryRepositoryTestConfig;
import com.gymcrm.workload.service.TrainerWorkloadService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Import(InMemoryRepositoryTestConfig.class)
@DisplayName("Trainer workload messaging - real Spring context + embedded broker")
class TrainerWorkloadMessagingIntegrationTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    //raw factory, not CachingConnectionFactory:
    //CachingConnectionFactory caches MessageConsumer
    //consumer from a cached session "won't get closed until the session is eventually removed from the pool"
    //so drain()/receive() leave consumer subscribed to the queue @JmsListener consumes from, competing for message
    @Autowired
    @Qualifier("activeMQConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Autowired
    private TrainerWorkloadService trainerWorkloadService;

    @Value("${trainer-workload.jms.destination.workload-updates}")
    private String sourceDestination;

    @Value("${trainer-workload.jms.destination.workload-updates-dlq}")
    private String deadLetterDestination;

    @Value("${security.jwt.inter-service.secret}")
    private String jwtSecret;

    @Value("${spring.application.name}")
    private String applicationName;

    private JmsTemplate receivingTemplate;

    @BeforeEach
    @AfterEach
    void resetQueues() {
        receivingTemplate = new JmsTemplate(connectionFactory);
        receivingTemplate.setReceiveTimeout(3000);
        drain(sourceDestination);
        drain(deadLetterDestination);
    }

    private void drain(String destination) {
        JmsTemplate noWait = new JmsTemplate(connectionFactory);
        noWait.setReceiveTimeout(50);
        while (noWait.receive(destination) != null) {
        }
    }

    @Test
    @DisplayName("a valid, authenticated message published to the queue is consumed and applied")
    void validMessage_getsApplied() {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "integration.trainer", "Ann", "Lee", true,
                LocalDate.of(2026, 6, 1), 90, ActionType.ADD);

        jmsTemplate.convertAndSend(sourceDestination, request,
                message -> {
                    message.setStringProperty("serviceAuthToken", validServiceToken());
                    return message;
                });

        MonthlyTrainingHoursResponse summary = awaitMonthlySummary("integration.trainer", 2026, 6);

        assertEquals(90, summary.trainingSummaryDuration());
    }

    @Test
    @DisplayName("a message with no service auth token ends up on the dead letter queue, never applied")
    void unauthenticatedMessage_isDeadLettered() throws JMSException {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "dlq.trainer", "Dan", "Lee", true,
                LocalDate.of(2026, 6, 1), 30, ActionType.ADD);

        jmsTemplate.convertAndSend(sourceDestination, request);

        Message dlqMessage = awaitMessage(deadLetterDestination);
        assertNotNull(dlqMessage, "expected the invalid message to arrive on the dead letter queue");
        assertEquals(sourceDestination, dlqMessage.getStringProperty("originalDestination"));
        assertNotNull(dlqMessage.getStringProperty("dlqReason"));

        assertThrows(TrainerWorkloadNotFoundException.class,
                () -> trainerWorkloadService.getMonthlySummary("dlq.trainer", 2026, 6));
    }

    @Test
    @DisplayName("a message missing required data ends up on the dead letter queue, never applied")
    void invalidPayload_isDeadLettered() throws JMSException {
        TrainerWorkloadRequest invalid = new TrainerWorkloadRequest(
                "", "No", "Username", true,
                LocalDate.of(2026, 6, 1), 30, ActionType.ADD);

        jmsTemplate.convertAndSend(sourceDestination, invalid,
                message -> {
                    message.setStringProperty("serviceAuthToken", validServiceToken());
                    return message;
                });

        Message dlqMessage = awaitMessage(deadLetterDestination);
        assertNotNull(dlqMessage, "expected the invalid message to arrive on the dead letter queue");
        assertNotNull(dlqMessage.getStringProperty("dlqReason"));
    }

    private Message awaitMessage(String destination) throws JMSException {
        JmsTemplate pollTemplate = new JmsTemplate(connectionFactory);
        pollTemplate.setReceiveTimeout(100);

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Message message = pollTemplate.receive(destination);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    //async hand-off from the JMS listener thread to the repository
    //poll instead of assuming it's applied by the time convertAndSend() returns
    private MonthlyTrainingHoursResponse awaitMonthlySummary(String username, int year, int month) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try {
                return trainerWorkloadService.getMonthlySummary(username, year, month);
            } catch (TrainerWorkloadNotFoundException notAppliedYet) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    fail("interrupted while waiting for the message to be applied");
                }
            }
        }
        return fail("message for " + username + " was not applied within 5s - " + describeDeadLetterQueueState());
    }

    //diagnostic, runs on the failure path to tells why it wasn't applied(rejected or never arrived)
    private String describeDeadLetterQueueState() {
        try {
            JmsTemplate peekTemplate = new JmsTemplate(connectionFactory);
            peekTemplate.setReceiveTimeout(200);
            Message dlqMessage = peekTemplate.receive(deadLetterDestination);
            if (dlqMessage == null) {
                return "and nothing on the dead letter queue either "
                        + "(message may never have reached the listener, or is still being redelivered)";
            }
            return "found on the dead letter queue instead, reason: "
                    + dlqMessage.getStringProperty("dlqReason");
        } catch (JMSException e) {
            return "and the dead letter queue could not be inspected: " + e.getMessage();
        }
    }

    private String validServiceToken() {
        SecretKey signingKey =
                Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject("gym-crm-service")
                .claim("scope", "INTERNAL_SERVICE")
                .claim("targetService", applicationName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
