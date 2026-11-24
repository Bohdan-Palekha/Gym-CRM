package com.gymcrm.workload.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Total training minutes recorded for one month")
public record MonthSummary(

        @Schema(description = "Month number, 1-12", example = "8")
        int month,

        @Schema(description = "Total training duration for the month, in minutes", example = "180")
        int trainingSummaryDuration
) {
}
