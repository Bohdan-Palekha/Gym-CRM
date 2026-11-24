package com.gymcrm.client.security;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceAuthTokenMessagePostProcessor")
class ServiceAuthTokenMessagePostProcessorTest {

    @Mock
    private InterServiceTokenProvider tokenProvider;

    @Test
    @DisplayName("attaches a freshly generated token as the serviceAuthToken JMS property")
    void attachesServiceAuthTokenProperty() throws JMSException {
        when(tokenProvider.generateToken()).thenReturn("signed-jwt-value");
        Message message = mock(Message.class);

        Message result = new ServiceAuthTokenMessagePostProcessor(tokenProvider).postProcessMessage(message);

        verify(message).setStringProperty(
                ServiceAuthTokenMessagePostProcessor.SERVICE_AUTH_TOKEN_PROPERTY, "signed-jwt-value");
        assertSame(message, result, "the same message instance should be returned for further chaining");
    }

    @Test
    @DisplayName("mints a new token on every call, never reuses a stale one")
    void mintsFreshTokenPerCall() throws JMSException {
        when(tokenProvider.generateToken()).thenReturn("token-1", "token-2");
        Message first = mock(Message.class);
        Message second = mock(Message.class);
        ServiceAuthTokenMessagePostProcessor postProcessor = new ServiceAuthTokenMessagePostProcessor(tokenProvider);

        postProcessor.postProcessMessage(first);
        postProcessor.postProcessMessage(second);

        verify(first).setStringProperty(ServiceAuthTokenMessagePostProcessor.SERVICE_AUTH_TOKEN_PROPERTY, "token-1");
        verify(second).setStringProperty(
                ServiceAuthTokenMessagePostProcessor.SERVICE_AUTH_TOKEN_PROPERTY, "token-2");
    }
}
