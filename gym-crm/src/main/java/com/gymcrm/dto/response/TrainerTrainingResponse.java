package com.gymcrm.dto.response;

import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

//REST task 13, single entry in get trainer list response
//trainer name is single field by spec, rendered as "firstname lastname"
//relation fixes hal key when this type is returned as a trainer training response collection,
@Schema(description = "Training entry, as seen from a trainer's trainings list")
@Relation(value = "training", collectionRelation = "trainings")
public record TrainerTrainingResponse(

        @Schema(description = "Training name", example = "Morning Yoga")
        String trainingName,

        @Schema(description = "Training date")
        LocalDate trainingDate,

        @Schema(description = "Training type")
        TrainingType.TrainingTypeName trainingType,

        @Schema(description = "Training duration in minutes", example = "60")
        int trainingDuration,

        @Schema(description = "Trainee full name", example = "Alice Nguyen")
        String traineeName
) {
    public static TrainerTrainingResponse from(Training training) {
        return new TrainerTrainingResponse(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(),
                training.getDurationMinutes(),
                training.getTrainee().getFirstName() + " " + training.getTrainee().getLastName());
    }
}
