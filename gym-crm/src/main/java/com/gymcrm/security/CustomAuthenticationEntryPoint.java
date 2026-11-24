package com.gymcrm.security;

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

//security task 2
//401 for protected endpoint with no valid Bearer token
//runs in security filter chain, before DispatcherServlet, so global exception handler doesn't see it
//thus same shaped ErrorResponse is written here
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
                "Full authentication is required. Provide a valid Bearer token from POST /api/auth/login.");
    }
}
