package com.gymcrm.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

//per REST note 17 there are two logging levels for every request
//transaction level generates or uses supplied transaction id, stores it in slf4j mdc
//so every log line made during request handling carries it and echoes it as a response header
//thus it's possible to hand it to downstream services
//per-call details — logs which endpoint was called, the request payload, and resulting status/response
//runs first, wrapping spring security filter chain (spring boot registers it with fixed default order
//after HIGHEST_PRECEDENCE),rejected request is fully logged inc. transactions
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionLoggingFilter extends OncePerRequestFilter {

    public static final String TRANSACTION_ID_MDC_KEY = "transactionId";
    //public, reused by client.security.TransactionIdMessagePostProcessor
    //same unchanged transactionId is forwarded to trainer-workload-service as a JMS message property
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    private static final int MAX_LOGGED_BODY_CHARS = 2000;

    //redaction of sensitive fields in json at HTTP
    //addition to existing sensitive AOP masking at service layer
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|oldPassword|newPassword|token)\"\\s*:\\s*\")[^\"]*(\")");


    //creds are now in json body, but guards exist for a future or misuse
    private static final Pattern SENSITIVE_QUERY_PARAM_PATTERN = Pattern.compile(
            "(?i)((?:^|[?&])(?:password|oldPassword|newPassword)=)[^&\\s]*");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String transactionId = Optional.ofNullable(request.getHeader(TRANSACTION_ID_HEADER))
                .filter(header -> !header.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId);
        response.setHeader(TRANSACTION_ID_HEADER, transactionId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String query = request.getQueryString();
        log.info("--> {} {}{}", request.getMethod(), request.getRequestURI(),
                query == null ? "" : "?" + maskQuery(query));

        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            int status = wrappedResponse.getStatus();
            String requestBody = maskJsonBody(bodyAsString(wrappedRequest.getContentAsByteArray()));
            String responseBody = maskJsonBody(bodyAsString(wrappedResponse.getContentAsByteArray()));

            if (status >= 400) {
                log.warn("<-- {} {} | status={} | {}ms | request=[{}] | response=[{}]",
                        request.getMethod(), request.getRequestURI(), status, durationMs,
                        requestBody, responseBody);
            } else {
                log.info("<-- {} {} | status={} | {}ms | response=[{}]",
                        request.getMethod(), request.getRequestURI(), status, durationMs, responseBody);
            }

            //since we buffer the response body to log it, we manually flush that buffer back to the client
            //or the client receives an empty response
            wrappedResponse.copyBodyToResponse();
            MDC.remove(TRANSACTION_ID_MDC_KEY);
        }
    }

    private String bodyAsString(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        String text = new String(content, StandardCharsets.UTF_8);
        return text.length() > MAX_LOGGED_BODY_CHARS
                ? text.substring(0, MAX_LOGGED_BODY_CHARS) + "...(truncated)"
                : text;
    }

    private String maskJsonBody(String body) {
        if (body.isEmpty()) {
            return body;
        }
        return SENSITIVE_FIELD_PATTERN.matcher(body).replaceAll("$1***$2");
    }

    private String maskQuery(String query) {
        return SENSITIVE_QUERY_PARAM_PATTERN.matcher(query).replaceAll("$1***");
    }
}
