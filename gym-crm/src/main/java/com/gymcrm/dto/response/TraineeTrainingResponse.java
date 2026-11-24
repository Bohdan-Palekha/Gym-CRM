package com.gymcrm.dto.response;

import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

//REST task 12, single entry in get trainee list response
//trainer name is single field by spec, rendered as "firstname lastname"
@Schema(description = "Training entry, as seen from a trainee's trainings list")
@Relation(value = "training", collectionRelation = "trainings")
public record TraineeTrainingResponse(

        @Schema(description = "Training name", example = "Morning Yoga")
        String trainingName,

        @Schema(description = "Training date")
        LocalDate trainingDate,

        @Schema(description = "Training type")
        TrainingType.TrainingTypeName trainingType,

        @Schema(description = "Training duration in minutes", example = "60")
        int trainingDuration,

        @Schema(description = "Trainer full name", example = "Marcus Lee")
        String trainerName
) {
    public static TraineeTrainingResponse from(Training training) {
        return new TraineeTrainingResponse(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(),
                training.getDurationMinutes(),
                training.getTrainer().getFirstName() + " " + training.getTrainer().getLastName());
    }
}
