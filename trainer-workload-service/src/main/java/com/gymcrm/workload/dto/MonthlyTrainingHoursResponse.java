package com.gymcrm.workload.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A trainer's total training minutes for one specific month")
public record MonthlyTrainingHoursResponse(

        @Schema(description = "Trainer's unique username", example = "Bob.Jones")
        String trainerUsername,

        @Schema(description = "Trainer's first name", example = "Bob")
        String trainerFirstName,

        @Schema(description = "Trainer's last name", example = "Jones")
        String trainerLastName,

        @Schema(description = "Trainer's status as of the most recent recorded training event", example = "true")
        boolean trainerStatus,

        @Schema(description = "Calendar year requested", example = "2026")
        int year,

        @Schema(description = "Month requested, 1-12", example = "8")
        int month,

        @Schema(description = "Total training duration for that month, in minutes "
                + "(0 if nothing was recorded)", example = "180")
        int trainingSummaryDuration
) {
}
