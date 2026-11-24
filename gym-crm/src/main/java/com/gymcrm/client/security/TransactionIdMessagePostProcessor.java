package com.gymcrm.client.security;

import com.gymcrm.logging.TransactionLoggingFilter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.MDC;
import org.springframework.jms.core.MessagePostProcessor;

//JMS counterpart of the removed TransactionIdPropagationInterceptor
//JMS message property replaces the X-Transaction-Id HTTP header
//transactionId ties log lines in both services to one operation
public class TransactionIdMessagePostProcessor implements MessagePostProcessor {

    public static final String TRANSACTION_ID_PROPERTY = TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY;

    @Override
    public Message postProcessMessage(Message message) throws JMSException {
        String transactionId = MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY);
        if (transactionId != null && !transactionId.isBlank()) {
            message.setStringProperty(TRANSACTION_ID_PROPERTY, transactionId);
        }
        return message;
    }
}
