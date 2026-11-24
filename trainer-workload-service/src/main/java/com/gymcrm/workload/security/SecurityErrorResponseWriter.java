package com.gymcrm.workload.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.workload.dto.ErrorResponse;
import com.gymcrm.workload.logging.TransactionLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

//shared by CustomAuthenticationEntryPoint and CustomAccessDeniedHandler
//security filter chain is ahead of the dispatcher servlet
//so GlobalExceptionHandler can't see them
//thus we write the same ErrorResponse json by hand
//mirrors gym-crm's security.SecurityErrorResponseWriter
final class SecurityErrorResponseWriter {

    private SecurityErrorResponseWriter() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response, ObjectMapper objectMapper,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY));

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
