package com.gymcrm.workload.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A trainer's full training-hours history, grouped by year and month")
public record TrainerWorkloadResponse(

        @Schema(description = "Trainer's unique username", example = "Bob.Jones")
        String trainerUsername,

        @Schema(description = "Trainer's first name", example = "Bob")
        String trainerFirstName,

        @Schema(description = "Trainer's last name", example = "Jones")
        String trainerLastName,

        @Schema(description = "Trainer's status as of the most recent recorded training event", example = "true")
        boolean trainerStatus,

        @Schema(description = "Training summaries grouped by year, most recent first")
        List<YearSummary> years
) {
}
