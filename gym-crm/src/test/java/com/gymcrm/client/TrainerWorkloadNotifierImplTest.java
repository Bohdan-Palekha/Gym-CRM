package com.gymcrm.client;

import com.gymcrm.client.dto.ActionType;
import com.gymcrm.client.dto.TrainerWorkloadRequest;
import com.gymcrm.client.security.InterServiceTokenProvider;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerWorkloadNotifierImpl (JMS)")
class TrainerWorkloadNotifierImplTest {

    private static final String DESTINATION = "trainer-workload.updates.test";

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private InterServiceTokenProvider tokenProvider;

    private TrainerWorkloadNotifierImpl notifier;

    @BeforeEach
    void setUp() {
        notifier = new TrainerWorkloadNotifierImpl(jmsTemplate, tokenProvider, DESTINATION);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private Training sampleTraining() {
        Trainer trainer = new Trainer();
        trainer.setUsername("john.smith");
        trainer.setFirstName("John");
        trainer.setLastName("Smith");
        trainer.setActive(true);

        Training training = new Training();
        training.setTrainer(trainer);
        training.setTrainingDate(LocalDate.of(2026, 3, 1));
        training.setDurationMinutes(60);
        return training;
    }

    @Test
    @DisplayName("trainingAdded publishes a request with ActionType.ADD to the configured destination")
    void trainingAdded_sendsAddRequest() {
        notifier.trainingAdded(sampleTraining());

        ArgumentCaptor<TrainerWorkloadRequest> captor = ArgumentCaptor.forClass(TrainerWorkloadRequest.class);
        verify(jmsTemplate).convertAndSend(eq(DESTINATION), captor.capture(), any(MessagePostProcessor.class));
        TrainerWorkloadRequest request = captor.getValue();
        assertEquals("john.smith", request.trainerUsername());
        assertEquals("John", request.trainerFirstName());
        assertEquals("Smith", request.trainerLastName());
        assertTrue(request.isActive());
        assertEquals(LocalDate.of(2026, 3, 1), request.trainingDate());
        assertEquals(60, request.trainingDuration());
        assertEquals(ActionType.ADD, request.actionType());
    }

    @Test
    @DisplayName("trainingDeleted publishes a request with ActionType.DELETE")
    void trainingDeleted_sendsDeleteRequest() {
        notifier.trainingDeleted(sampleTraining());

        ArgumentCaptor<TrainerWorkloadRequest> captor = ArgumentCaptor.forClass(TrainerWorkloadRequest.class);
        verify(jmsTemplate).convertAndSend(eq(DESTINATION), captor.capture(), any(MessagePostProcessor.class));
        assertEquals(ActionType.DELETE, captor.getValue().actionType());
    }

    @Test
    @DisplayName("the outgoing message is enriched with a fresh service token and the MDC transaction id")
    void send_enrichesMessageWithTokenAndTransactionId() throws JMSException {
        when(tokenProvider.generateToken()).thenReturn("signed-jwt");
        MDC.put("transactionId", "txn-42");
        Message jmsMessage = mock(Message.class);

        notifier.trainingAdded(sampleTraining());

        ArgumentCaptor<MessagePostProcessor> ppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq(DESTINATION), any(TrainerWorkloadRequest.class), ppCaptor.capture());
        ppCaptor.getValue().postProcessMessage(jmsMessage);

        verify(jmsMessage).setStringProperty("serviceAuthToken", "signed-jwt");
        verify(jmsMessage).setStringProperty("transactionId", "txn-42");
    }

    @Test
    @DisplayName("a broker/send failure is caught and logged, never propagated to the caller")
    void send_whenBrokerUnreachable_doesNotThrow() {
        doThrow(new JmsException("broker unreachable") { })
                .when(jmsTemplate)
                .convertAndSend(eq(DESTINATION), any(TrainerWorkloadRequest.class), any(MessagePostProcessor.class));

        assertDoesNotThrow(() -> notifier.trainingAdded(sampleTraining()));
    }
}
