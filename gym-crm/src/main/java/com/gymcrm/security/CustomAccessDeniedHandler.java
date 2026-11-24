package com.gymcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//counterpart to  CustomAuthenticationEntryPoint
//currently unused
//kept for a uniform ErrorResponse instead of spring security default 403 page if ever needed
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        log.warn("403 Forbidden on {} {}: {}", request.getMethod(), request.getRequestURI(),
                accessDeniedException.getMessage());

        SecurityErrorResponseWriter.write(request, response, objectMapper, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.");
    }
}
