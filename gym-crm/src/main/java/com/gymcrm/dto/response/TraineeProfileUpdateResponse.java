package com.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymcrm.model.Trainee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

//REST task 6
//by spec does include username unlike profile response
@Schema(description = "Updated trainee profile")
public record TraineeProfileUpdateResponse(

        @Schema(description = "Username", example = "alice.nguyen")
        String username,

        @Schema(description = "First name", example = "Alice")
        String firstName,

        @Schema(description = "Last name", example = "Nguyen")
        String lastName,

        @Schema(description = "Date of birth")
        LocalDate dateOfBirth,

        @Schema(description = "Home address")
        String address,

        @JsonProperty("isActive")
        @Schema(description = "Whether the trainee profile is active")
        boolean isActive,

        @Schema(description = "Trainers currently assigned to this trainee")
        List<TrainerSummary> trainers
) {
    public static TraineeProfileUpdateResponse from(Trainee trainee) {
        return new TraineeProfileUpdateResponse(
                trainee.getUsername(),
                trainee.getFirstName(),
                trainee.getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                trainee.isActive(),
                trainee.getTrainers().stream().map(TrainerSummary::from).toList());
    }
}
