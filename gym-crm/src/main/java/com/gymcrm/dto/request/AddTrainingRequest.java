package com.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

//REST task 14
//no training type field, it's derived from trainer's specialization
@Schema(description = "Add training request")
public record AddTrainingRequest(

        @Schema(description = "Trainee username", example = "alice.nguyen", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "traineeUsername is required")
        String traineeUsername,

        @Schema(description = "Trainer username", example = "marcus.lee", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "trainerUsername is required")
        String trainerUsername,

        @Schema(description = "Training session name", example = "Morning Yoga", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "trainingName is required")
        String trainingName,

        @Schema(description = "Date the training takes place", example = "2026-08-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "trainingDate is required")
        LocalDate trainingDate,

        @Schema(description = "Training duration in minutes", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "trainingDuration is required")
        @Positive(message = "trainingDuration must be positive")
        Integer trainingDuration
) {
}
