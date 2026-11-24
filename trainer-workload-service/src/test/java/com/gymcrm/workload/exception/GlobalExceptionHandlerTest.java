package com.gymcrm.workload.exception;

import com.gymcrm.workload.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestURI()).thenReturn("/api/trainer-workloads/bob.trainer");
        lenient().when(request.getMethod()).thenReturn("GET");
    }

    @Test
    @DisplayName("TrainerWorkloadNotFoundException maps to 404 with its own message")
    void notFound_mapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new TrainerWorkloadNotFoundException("No workload recorded for trainer: ghost"),
                        request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No workload recorded for trainer: ghost", response.getBody().message());
        assertEquals("/api/trainer-workloads/bob.trainer", response.getBody().path());
    }

    @Test
    @DisplayName("malformed request body maps to 400 with a generic message (no parser internals leaked)")
    void unreadableBody_mapsTo400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body.", response.getBody().message());
    }

    @Test
    @DisplayName("criterion 5.5: a NullPointerException never reaches the caller as a raw 500 — "
            + "it is sanitized to a generic message")
    void nullPointerException_isSanitized() {
        NullPointerException npe = new NullPointerException("some internal field was null, exposing this would leak internals");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(npe, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred.", response.getBody().message());
        assertFalse(response.getBody().message().contains("some internal field"),
                "the raw exception message must never be exposed to the caller");
    }

    @Test
    @DisplayName("criterion 5.5: any other unforeseen exception is equally sanitized to 500")
    void arbitraryRuntimeException_isSanitized() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("db pool exhausted: internal detail"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred.", response.getBody().message());
    }

    @Test
    @DisplayName("every error response carries the request path it originated from")
    void errorResponse_includesRequestPath() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new TrainerWorkloadNotFoundException("x"), request);

        assertEquals("/api/trainer-workloads/bob.trainer", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }
}
