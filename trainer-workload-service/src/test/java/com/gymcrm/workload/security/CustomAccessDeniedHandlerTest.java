package com.gymcrm.workload.security;

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
import org.springframework.security.access.AccessDeniedException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAccessDeniedHandler")
class CustomAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private CustomAccessDeniedHandler handler;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new CustomAccessDeniedHandler(objectMapper);

        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/trainer-workloads");
    }

    @Test
    @DisplayName("writes a 403 JSON body with status and path")
    void writesA403Body() throws Exception {
        handler.handle(request, response, new AccessDeniedException("Access is denied"));

        verify(response).setStatus(403);
        verify(response).setContentType("application/json");

        String body = responseBody.toString();
        assertTrue(body.contains("\"status\":403"));
        assertTrue(body.contains("/api/trainer-workloads"));
    }
}
