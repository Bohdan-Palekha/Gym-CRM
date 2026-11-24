package com.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymcrm.model.Trainee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

//REST task 5
//by spec does not include username unlike update profile response
@Schema(description = "Trainee profile")
public record TraineeProfileResponse(

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
    public static TraineeProfileResponse from(Trainee trainee) {
        Set<com.gymcrm.model.Trainer> trainerSet = trainee.getTrainers();
        return new TraineeProfileResponse(
                trainee.getFirstName(),
                trainee.getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                trainee.isActive(),
                trainerSet.stream().map(TrainerSummary::from).toList());
    }
}
