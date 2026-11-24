package com.gymcrm.workload.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.workload.dto.ActionType;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JmsConfig (trainer-workload-service consumer)")
class JmsConfigTest {

    private final JmsConfig config = new JmsConfig();

    @Test
    @DisplayName("connection factory uses the configured broker URL and credentials")
    void connectionFactory_usesConfiguredBrokerUrlAndCredentials() {
        ActiveMQConnectionFactory factory =
                config.activeMQConnectionFactory("tcp://broker:61616", "admin", "secret", 3, 1000, 30000, 2.0);

        assertEquals("tcp://broker:61616", factory.getBrokerURL());
        assertEquals("admin", factory.getUserName());
        assertEquals("secret", factory.getPassword());
    }

    @Test
    @DisplayName("connection factory applies the configured redelivery policy for transient failures")
    void connectionFactory_appliesRedeliveryPolicy() {
        ActiveMQConnectionFactory factory =
                config.activeMQConnectionFactory("tcp://broker:61616", "", "", 5, 2000, 60000, 3.0);

        RedeliveryPolicy policy = factory.getRedeliveryPolicy();
        assertEquals(5, policy.getMaximumRedeliveries());
        assertEquals(2000, policy.getInitialRedeliveryDelay());
        assertEquals(60000, policy.getMaximumRedeliveryDelay());
        assertTrue(policy.isUseExponentialBackOff());
        assertEquals(3.0, policy.getBackOffMultiplier());
    }

    @Test
    @DisplayName("JmsTemplate's connection factory wraps the target factory with caching")
    void jmsConnectionFactory_wrapsTargetWithCaching() {
        ActiveMQConnectionFactory target = new ActiveMQConnectionFactory("tcp://broker:61616");

        CachingConnectionFactory caching = config.jmsConnectionFactory(target);

        assertSame(target, caching.getTargetConnectionFactory());
    }

    @Test
    @DisplayName("message converter writes JSON as a TextMessage, with a synthetic (non-FQCN) type-id property")
    void messageConverter_producesPlainJsonTextWithSyntheticTypeId() throws JMSException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter converter = config.jacksonJmsMessageConverter(objectMapper);
        Session session = mock(Session.class);
        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "john.smith", "John", "Smith", true, LocalDate.of(2026, 3, 1), 60, ActionType.ADD);

        converter.toMessage(request, session);

        verify(session).createTextMessage(anyString());
        verify(textMessage).setStringProperty("payloadType", JmsConfig.PAYLOAD_TYPE_ID);
        verify(textMessage).setStringProperty(anyString(), anyString());
    }

    @Test
    @DisplayName("REGRESSION: the converter can read back a message it wrote, through a real JMS session "
            + "(a null type-id property name previously made every incoming message throw)")
    void messageConverter_roundTripsSendAndReceiveThroughRealSession() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter converter = config.jacksonJmsMessageConverter(objectMapper);
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "john.smith", "John", "Smith", true, LocalDate.of(2026, 3, 1), 60, ActionType.ADD);
        ActiveMQConnectionFactory embeddedFactory = new ActiveMQConnectionFactory(
                "vm://jms-config-test-broker-workload?broker.persistent=false&broker.useJmx=false");

        try (Connection connection = embeddedFactory.createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Message written = converter.toMessage(request, session);
            Object roundTripped = converter.fromMessage(written);

            assertEquals(request, roundTripped);
        }
    }

    @Test
    @DisplayName("listener container factory uses the raw (uncached) connection factory, transacted sessions, "
            + "and a non-null error handler")
    void listenerContainerFactory_isConfiguredCorrectly() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://broker:61616");
        MappingJackson2MessageConverter converter = config.jacksonJmsMessageConverter(new ObjectMapper());

        DefaultJmsListenerContainerFactory factory =
                config.trainerWorkloadJmsListenerContainerFactory(connectionFactory, converter);
        DefaultMessageListenerContainer container = factory.createListenerContainer(mock(JmsListenerEndpoint.class));

        assertSame(connectionFactory, container.getConnectionFactory());
        assertTrue(container.isSessionTransacted());
        assertNotNull(container.getErrorHandler());
    }
}
