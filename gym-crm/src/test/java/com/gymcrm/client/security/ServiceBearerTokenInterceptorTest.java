package com.gymcrm.client.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

//task 7
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceBearerTokenInterceptor")
class ServiceBearerTokenInterceptorTest {

    @Mock
    private InterServiceTokenProvider tokenProvider;
    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse response;

    @Test
    @DisplayName("attaches a freshly generated token as an Authorization: Bearer header")
    void attachesBearerToken() throws IOException {
        when(tokenProvider.generateToken()).thenReturn("signed-jwt-value");
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        new ServiceBearerTokenInterceptor(tokenProvider).intercept(request, new byte[0], execution);

        assertEquals("Bearer signed-jwt-value", headers.getFirst(HttpHeaders.AUTHORIZATION));
        verify(execution).execute(request, new byte[0]);
    }
}
