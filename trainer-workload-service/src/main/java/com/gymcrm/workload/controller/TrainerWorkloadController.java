package com.gymcrm.workload.controller;

import com.gymcrm.workload.dto.ErrorResponse;
import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadResponse;
import com.gymcrm.workload.service.TrainerWorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Richardson maturity level 2 per task
//gym-crm publishes training add/delete events over ActiveMQ
@Slf4j
@RestController
@RequestMapping("/api/trainer-workloads")
@RequiredArgsConstructor
@Validated
@Tag(name = "Trainer Workloads", description = "Per-trainer monthly training-hours summaries")
public class TrainerWorkloadController {

    private final TrainerWorkloadService trainerWorkloadService;

    @Operation(summary = "Get a trainer's total training minutes for one month",
            description = "MicroserviceTask  4: \"requesting the number of training "
                    + "hours from any of the trainers in a particular month\".")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @ApiResponse(responseCode = "404", description = "No workload recorded for this trainer",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{username}/years/{year}/months/{month}")
    public ResponseEntity<MonthlyTrainingHoursResponse> getMonthlySummary(
            @PathVariable String username,
            @PathVariable @Min(2000) @Max(2100) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(trainerWorkloadService.getMonthlySummary(username, year, month));
    }

    @Operation(summary = "Get a trainer's full training-hours history",
            description = "Full Years -> Months breakdown, matching the model in "
                    + "MicroserviceTask  3.")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @ApiResponse(responseCode = "404", description = "No workload recorded for this trainer",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{username}")
    public ResponseEntity<TrainerWorkloadResponse> getFullSummary(@PathVariable String username) {
        return ResponseEntity.ok(trainerWorkloadService.getFullSummary(username));
    }
}
