package com.gymcrm.client.security;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("TransactionIdMessagePostProcessor")
class TransactionIdMessagePostProcessorTest {

    private final TransactionIdMessagePostProcessor postProcessor = new TransactionIdMessagePostProcessor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("forwards the current MDC transactionId as a JMS property")
    void forwardsTransactionIdWhenPresent() throws JMSException {
        MDC.put("transactionId", "txn-123");
        Message message = mock(Message.class);

        Message result = postProcessor.postProcessMessage(message);

        verify(message).setStringProperty(TransactionIdMessagePostProcessor.TRANSACTION_ID_PROPERTY, "txn-123");
        assertSame(message, result);
    }

    @Test
    @DisplayName("leaves the message untouched when no transactionId is in MDC")
    void skipsPropertyWhenMdcEmpty() throws JMSException {
        Message message = mock(Message.class);

        postProcessor.postProcessMessage(message);

        verify(message, never())
                .setStringProperty(eq(TransactionIdMessagePostProcessor.TRANSACTION_ID_PROPERTY), anyString());
    }

    @Test
    @DisplayName("leaves the message untouched when the MDC transactionId is blank")
    void skipsPropertyWhenMdcBlank() throws JMSException {
        MDC.put("transactionId", "   ");
        Message message = mock(Message.class);

        postProcessor.postProcessMessage(message);

        verify(message, never())
                .setStringProperty(eq(TransactionIdMessagePostProcessor.TRANSACTION_ID_PROPERTY), anyString());
    }
}
