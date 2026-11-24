package com.gymcrm.client.security;

import com.gymcrm.logging.TransactionLoggingFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

//id put in MDC for the inbound request is forwarded, transactionId ties log line across both services for one operation
public class TransactionIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String transactionId = MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY);
        if (transactionId != null && !transactionId.isBlank()) {
            request.getHeaders().set(TransactionLoggingFilter.TRANSACTION_ID_HEADER, transactionId);
        }
        return execution.execute(request, body);
    }
}
