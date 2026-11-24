package com.gymcrm.workload.messaging;

import com.gymcrm.workload.dto.ActionType;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.security.ServiceTokenValidator;
import com.gymcrm.workload.service.TrainerWorkloadService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerWorkloadUpdateListener")
class TrainerWorkloadUpdateListenerTest {

    private static final String SOURCE = "trainer-workload.updates.test";
    private static final String DLQ = "trainer-workload.updates.dlq.test";

    @Mock
    private TrainerWorkloadService trainerWorkloadService;
    @Mock
    private ServiceTokenValidator tokenValidator;
    @Mock
    private JmsTemplate jmsTemplate;

    private TrainerWorkloadUpdateListener listener;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        listener = new TrainerWorkloadUpdateListener(
                trainerWorkloadService, tokenValidator, validator, jmsTemplate, SOURCE, DLQ);
    }

    private TrainerWorkloadRequest validRequest() {
        return new TrainerWorkloadRequest(
                "bob.trainer", "Bob", "Jones", true, LocalDate.of(2026, 8, 15), 60, ActionType.ADD);
    }

    @Test
    @DisplayName("a valid, authenticated message is applied and never dead-lettered")
    void validMessage_isApplied() {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));

        listener.onWorkloadUpdate(validRequest(), "good-token", "txn-1");

        verify(trainerWorkloadService).applyWorkloadChange(validRequest());
        verifyNoInteractions(jmsTemplate);
    }

    @Test
    @DisplayName("a missing service auth token is dead-lettered instead of applied")
    void missingToken_isDeadLettered() {
        when(tokenValidator.validate(null)).thenReturn(Optional.empty());

        listener.onWorkloadUpdate(validRequest(), null, "txn-2");

        verifyNoInteractions(trainerWorkloadService);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(validRequest()), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("an invalid service auth token is dead-lettered instead of applied")
    void invalidToken_isDeadLettered() {
        when(tokenValidator.validate("bad-token")).thenReturn(Optional.empty());

        listener.onWorkloadUpdate(validRequest(), "bad-token", null);

        verifyNoInteractions(trainerWorkloadService);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(validRequest()), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("a blank trainerUsername (required information missing) is dead-lettered instead of applied")
    void blankUsername_isDeadLettered() {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));
        TrainerWorkloadRequest invalid = new TrainerWorkloadRequest(
                "", "Bob", "Jones", true, LocalDate.of(2026, 8, 15), 60, ActionType.ADD);

        listener.onWorkloadUpdate(invalid, "good-token", null);

        verifyNoInteractions(trainerWorkloadService);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(invalid), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("a non-positive trainingDuration is dead-lettered instead of applied")
    void nonPositiveDuration_isDeadLettered() {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));
        TrainerWorkloadRequest invalid = new TrainerWorkloadRequest(
                "bob.trainer", "Bob", "Jones", true, LocalDate.of(2026, 8, 15), 0, ActionType.ADD);

        listener.onWorkloadUpdate(invalid, "good-token", null);

        verifyNoInteractions(trainerWorkloadService);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(invalid), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("a null message payload is dropped, never applied and never dead-lettered")
    void nullRequest_isDroppedWithoutApplyingOrDeadLettering() {
        listener.onWorkloadUpdate(null, "good-token", null);

        verifyNoInteractions(trainerWorkloadService);
        verifyNoInteractions(jmsTemplate);
        verifyNoInteractions(tokenValidator);
    }

    @Test
    @DisplayName("the dead-letter message carries the rejection reason, source destination and original transaction id")
    void deadLetter_carriesDiagnosticProperties() throws JMSException {
        when(tokenValidator.validate(null)).thenReturn(Optional.empty());
        Message dlqMessage = mock(Message.class);

        listener.onWorkloadUpdate(validRequest(), null, "txn-3");

        ArgumentCaptor<MessagePostProcessor> ppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(validRequest()), ppCaptor.capture());
        ppCaptor.getValue().postProcessMessage(dlqMessage);

        verify(dlqMessage).setStringProperty(eq("dlqReason"), contains("service authentication token"));
        verify(dlqMessage).setStringProperty("originalDestination", SOURCE);
        verify(dlqMessage).setStringProperty("transactionId", "txn-3");
        verify(dlqMessage).setStringProperty(eq("failedAt"), anyString());
        verify(dlqMessage, never()).setStringProperty(eq(TrainerWorkloadUpdateListener.SERVICE_AUTH_TOKEN_PROPERTY), anyString());
    }

    @Test
    @DisplayName("the dead-letter message also preserves a present (but rejected-payload) auth token, and omits a missing transaction id")
    void deadLetter_preservesPresentTokenAndOmitsMissingTransactionId() throws JMSException {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));
        TrainerWorkloadRequest invalid = new TrainerWorkloadRequest(
                "", "Bob", "Jones", true, LocalDate.of(2026, 8, 15), 60, ActionType.ADD);
        Message dlqMessage = mock(Message.class);

        listener.onWorkloadUpdate(invalid, "good-token", null);

        ArgumentCaptor<MessagePostProcessor> ppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(invalid), ppCaptor.capture());
        ppCaptor.getValue().postProcessMessage(dlqMessage);

        verify(dlqMessage).setStringProperty(TrainerWorkloadUpdateListener.SERVICE_AUTH_TOKEN_PROPERTY, "good-token");
        verify(dlqMessage, never()).setStringProperty(eq("transactionId"), anyString());
    }

    @Test
    @DisplayName("an unexpected runtime exception from the service layer propagates so the broker redelivers")
    void unexpectedServiceFailure_propagates() {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));
        doThrow(new IllegalStateException("boom")).when(trainerWorkloadService).applyWorkloadChange(any());

        assertThrows(IllegalStateException.class,
                () -> listener.onWorkloadUpdate(validRequest(), "good-token", null));

        verifyNoInteractions(jmsTemplate);
    }

    @Test
    @DisplayName("the transaction id is available in MDC while processing, and cleared afterward either way")
    void transactionId_isInMdcDuringProcessingAndClearedAfter() {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));
        doAnswer(invocation -> {
            assertEquals("txn-99", MDC.get("transactionId"));
            return null;
        }).when(trainerWorkloadService).applyWorkloadChange(any());

        listener.onWorkloadUpdate(validRequest(), "good-token", "txn-99");

        assertNull(MDC.get("transactionId"), "MDC should be cleared after the listener returns");
    }
}
