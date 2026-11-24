package com.gymcrm.client.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

//attaches short jwt to every call to workload service
//minting per call makes this stateless and makes sure no expired token is sent
public class ServiceBearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private final InterServiceTokenProvider tokenProvider;

    public ServiceBearerTokenInterceptor(InterServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.generateToken());
        return execution.execute(request, body);
    }
}
