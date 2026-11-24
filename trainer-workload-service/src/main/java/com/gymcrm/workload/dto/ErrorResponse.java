package com.gymcrm.workload.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

//same shape as gym-crm's dto.response.ErrorResponse
//transactionId lets a failure be traced across both logs by transactionId
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

        @Schema(description = "Path that was requested", example = "/api/trainer-workloads/unknown.user")
        String path,

        @Schema(description = "Transaction id correlating this request's log lines across both services")
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
            @Schema(description = "Name of the invalid field", example = "trainerUsername") String field,
            @Schema(description = "Why the field is invalid", example = "must not be blank") String message) {
    }
}
