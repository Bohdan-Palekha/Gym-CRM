package com.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.TrainingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

//REST task 8
//by spec does not include username unlike update profile response
@Schema(description = "Trainer profile")
public record TrainerProfileResponse(

        @Schema(description = "First name", example = "Marcus")
        String firstName,

        @Schema(description = "Last name", example = "Lee")
        String lastName,

        @Schema(description = "Specialization")
        TrainingType.TrainingTypeName specialization,

        @JsonProperty("isActive")
        @Schema(description = "Whether the trainer profile is active")
        boolean isActive,

        @Schema(description = "Trainees currently assigned to this trainer")
        List<TraineeSummary> trainees
) {
    public static TrainerProfileResponse from(Trainer trainer) {
        return new TrainerProfileResponse(
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getSpecialization().getTrainingTypeName(),
                trainer.isActive(),
                trainer.getTrainees().stream().map(TraineeSummary::from).toList());
    }
}
