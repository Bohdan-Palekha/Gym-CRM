package com.gymcrm.workload.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "One calendar year's monthly training summaries")
public record YearSummary(

        @Schema(description = "Calendar year", example = "2026")
        int year,

        @Schema(description = "Per-month summaries for this year, months with zero recorded "
                + "training minutes are omitted")
        List<MonthSummary> months
) {
}
