package com.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;


//uniform response for every failed request both caught by exception handler or written by auth filter
//auth filter runs before dispatcher servlet, cannot use rest controller advice
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public record ErrorResponse(

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "When the error occurred")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "Short HTTP status reason", example = "Not Found")
        String error,

        @Schema(description = "Human-readable error message")
        String message,

        @Schema(description = "Path that was requested", example = "/api/trainees/unknown.user")
        String path,

        @Schema(description = "Transaction id correlating this request's log lines")
        String transactionId,

        @Schema(description = "Per-field validation errors, when applicable")
        List<FieldErrorDetail> fieldErrors
) {

    public ErrorResponse(Instant timestamp, int status, String error, String message,
                          String path, String transactionId) {
        this(timestamp, status, error, message, path, transactionId, null);
    }

    @Schema(description = "A single field validation failure")
    public record FieldErrorDetail(
            @Schema(description = "Name of the invalid field", example = "firstName") String field,
            @Schema(description = "Why the field is invalid", example = "firstName is required") String message) {
    }
}
