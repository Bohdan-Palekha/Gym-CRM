package com.gymcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAuthenticationEntryPoint")
class CustomAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private CustomAuthenticationEntryPoint entryPoint;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        entryPoint = new CustomAuthenticationEntryPoint(objectMapper);

        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/trainees/alice.nguyen");
    }

    @Test
    @DisplayName("writes a 401 JSON body with status, path, and a message pointing at the login endpoint")
    void writesA401Body() throws Exception {
        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");

        String body = responseBody.toString();
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("/api/trainees/alice.nguyen"));
        assertTrue(body.contains("/api/auth/login"));
    }
}
