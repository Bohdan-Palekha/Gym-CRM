package com.gymcrm.workload.exception;

import com.gymcrm.workload.dto.ErrorResponse;
import com.gymcrm.workload.logging.TransactionLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

//centralized error handling, every failure is normalized to the same ErrorResponse shape as gym-crm
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TrainerWorkloadNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TrainerWorkloadNotFoundException ex,
                                                          HttpServletRequest request) {
        log.warn("404 Not Found on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        log.warn("400 Bad Request on {} {}: {} field validation error(s)",
                request.getMethod(), request.getRequestURI(), fieldErrors.size());
        return build(HttpStatus.BAD_REQUEST, "Request validation failed.", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                     HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(
                        v.getPropertyPath() == null ? "" : v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();
        log.warn("400 Bad Request on {} {}: {} constraint violation(s)",
                request.getMethod(), request.getRequestURI(), fieldErrors.size());
        return build(HttpStatus.BAD_REQUEST, "Request validation failed.", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        String message = "Parameter '%s' has an invalid value: %s".formatted(ex.getName(), ex.getValue());
        log.warn("400 Bad Request on {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        log.warn("400 Bad Request on {} {}: malformed request body", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body.", request, null);
    }

    //spring's default handler would resolve normally  but
    //RestControllerAdvice takes priority and would turn exception into 500
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                    HttpServletRequest request) {
        log.warn("405 Method Not Allowed on {} {}", request.getMethod(), request.getRequestURI());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (ex.getSupportedHttpMethods() != null) {
            response = response.allow(ex.getSupportedHttpMethods().toArray(new HttpMethod[0]));
        }
        return response.body(errorBody(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                                      HttpServletRequest request) {
        log.warn("415 Unsupported Media Type on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                                 HttpServletRequest request) {
        log.warn("404 Not Found on {} {}", request.getMethod(), request.getRequestURI());
        String message = "No endpoint matches " + request.getMethod() + " " + request.getRequestURI();
        return build(HttpStatus.NOT_FOUND, message, request, null);
    }

    //catch-all for nullPointerException or anything else
    //real exception is still logged server-side
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("500 Internal Server Error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request, null);
    }

    private ErrorResponse.FieldErrorDetail toFieldError(FieldError error) {
        return new ErrorResponse.FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                 List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        return ResponseEntity.status(status).body(errorBody(status, message, request, fieldErrors));
    }

    private ErrorResponse errorBody(HttpStatus status, String message, HttpServletRequest request,
                                     List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY),
                fieldErrors);
    }
}
