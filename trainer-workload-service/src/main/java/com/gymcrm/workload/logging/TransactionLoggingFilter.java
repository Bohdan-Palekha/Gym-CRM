package com.gymcrm.workload.logging;

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

//called endpoint, request payload, result status/response, all under transactionId, reused from X-Transaction-Id from gym-crm
//every log line in both services is bound by same id
//fresh one is minted only when this service is called directly with no header, for example from swaggerUi
//runs first wrapping security filter chain , thus 401/403 request is fully logged
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionLoggingFilter extends OncePerRequestFilter {

    public static final String TRANSACTION_ID_MDC_KEY = "transactionId";
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    private static final int MAX_LOGGED_BODY_CHARS = 2000;

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

        log.info("--> {} {}", request.getMethod(), request.getRequestURI());

        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            int status = wrappedResponse.getStatus();
            String requestBody = bodyAsString(wrappedRequest.getContentAsByteArray());
            String responseBody = bodyAsString(wrappedResponse.getContentAsByteArray());

            if (status >= 400) {
                log.warn("<-- {} {} | status={} | {}ms | request=[{}] | response=[{}]",
                        request.getMethod(), request.getRequestURI(), status, durationMs,
                        requestBody, responseBody);
            } else {
                log.info("<-- {} {} | status={} | {}ms | response=[{}]",
                        request.getMethod(), request.getRequestURI(), status, durationMs, responseBody);
            }

            //manual flush of response body so no empty body is received
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
}
