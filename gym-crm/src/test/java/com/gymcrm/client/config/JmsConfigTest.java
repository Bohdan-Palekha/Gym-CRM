package com.gymcrm.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.client.dto.ActionType;
import com.gymcrm.client.dto.TrainerWorkloadRequest;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JmsConfig (gym-crm producer)")
class JmsConfigTest {

    private final JmsConfig config = new JmsConfig();

    @Test
    @DisplayName("connection factory uses the configured broker URL")
    void connectionFactory_usesConfiguredBrokerUrl() {
        ActiveMQConnectionFactory factory = config.activeMQConnectionFactory("tcp://broker:61616", "", "");

        assertEquals("tcp://broker:61616", factory.getBrokerURL());
    }

    @Test
    @DisplayName("connection factory sets credentials when a user is configured")
    void connectionFactory_setsCredentialsWhenProvided() {
        ActiveMQConnectionFactory factory = config.activeMQConnectionFactory("tcp://broker:61616", "admin", "secret");

        assertEquals("admin", factory.getUserName());
        assertEquals("secret", factory.getPassword());
    }

    @Test
    @DisplayName("connection factory leaves credentials unset when none are configured (e.g. local dev)")
    void connectionFactory_leavesCredentialsUnsetWhenBlank() {
        ActiveMQConnectionFactory factory = config.activeMQConnectionFactory("tcp://broker:61616", "", "");

        assertNull(factory.getUserName());
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
    @DisplayName("REGRESSION: the converter can read back a message it wrote (a null type-id property name "
            + "previously made every incoming message throw). Deliberately mock-only, no real connection: "
            + "activemq-client-jakarta (required on Spring Boot 3.x) cannot share a JVM with an embedded "
            + "vm:// broker, so a real round trip belongs in the @SpringBootTest integration tests instead, "
            + "against a real tcp:// broker - see MESSAGING_MIGRATION.md 6.2.")
    void messageConverter_roundTripsSendAndReceive() throws JMSException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter converter = config.jacksonJmsMessageConverter(objectMapper);
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "john.smith", "John", "Smith", true, LocalDate.of(2026, 3, 1), 60, ActionType.ADD);
        Session session = mock(Session.class);
        TextMessage textMessage = mock(TextMessage.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createTextMessage(jsonCaptor.capture())).thenReturn(textMessage);

        Message written = converter.toMessage(request, session);
        assertSame(textMessage, written);

        when(textMessage.getText()).thenReturn(jsonCaptor.getValue());
        when(textMessage.getStringProperty("payloadType")).thenReturn(JmsConfig.PAYLOAD_TYPE_ID);

        Object roundTripped = converter.fromMessage(textMessage);

        assertEquals(request, roundTripped);
    }
}