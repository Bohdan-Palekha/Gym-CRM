package com.gymcrm.workload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Schema(description = "A single training session's effect on a trainer's monthly workload")
public record TrainerWorkloadRequest(

        @Schema(description = "Trainer's unique username", example = "Bob.Jones")
        @NotBlank(message = "trainerUsername must not be blank")
        String trainerUsername,

        @Schema(description = "Trainer's first name", example = "Bob")
        @NotBlank(message = "trainerFirstName must not be blank")
        String trainerFirstName,

        @Schema(description = "Trainer's last name", example = "Jones")
        @NotBlank(message = "trainerLastName must not be blank")
        String trainerLastName,

        @Schema(description = "Whether the trainer is currently active", example = "true")
        @JsonProperty("isActive")
        boolean isActive,

        @Schema(description = "Date the training session takes place", example = "2026-08-15")
        @NotNull(message = "trainingDate must not be null")
        LocalDate trainingDate,

        @Schema(description = "Training duration in minutes", example = "60")
        @Positive(message = "trainingDuration must be positive")
        int trainingDuration,

        @Schema(description = "Whether this training was added or cancelled", example = "ADD")
        @NotNull(message = "actionType must not be null")
        ActionType actionType
) {
}
