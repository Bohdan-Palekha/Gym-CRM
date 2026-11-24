package com.gymcrm.workload.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

//401 for any request missing a valid inter-service bearer token
//runs in the security filter chain, before the dispatcher servlet, so GlobalExceptionHandler never sees it
//thus same shaped ErrorResponse is written
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        log.warn("401 Unauthorized on {} {}: {}", request.getMethod(), request.getRequestURI(),
                authException.getMessage());

        SecurityErrorResponseWriter.write(request, response, objectMapper, HttpStatus.UNAUTHORIZED,
                "Full authentication is required. Provide a valid inter-service Bearer token.");
    }
}
