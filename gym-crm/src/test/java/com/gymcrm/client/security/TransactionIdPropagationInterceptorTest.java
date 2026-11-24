package com.gymcrm.client.security;

import com.gymcrm.logging.TransactionLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionIdPropagationInterceptor")
class TransactionIdPropagationInterceptorTest {

    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse response;

    private final TransactionIdPropagationInterceptor interceptor = new TransactionIdPropagationInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("forwards the current MDC transaction id as X-Transaction-Id")
    void mdcPresent_forwardsHeader() throws IOException {
        MDC.put(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY, "txn-123");
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("txn-123", headers.getFirst(TransactionLoggingFilter.TRANSACTION_ID_HEADER));
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    @DisplayName("no active MDC transaction id -> no header is added, request still proceeds")
    void noMdc_doesNotAddHeader() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        verify(request, never()).getHeaders();
        verify(execution).execute(request, new byte[0]);
    }
}
