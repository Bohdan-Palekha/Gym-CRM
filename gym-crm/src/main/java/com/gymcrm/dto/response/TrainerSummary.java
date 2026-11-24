package com.gymcrm.dto.response;

import com.gymcrm.model.Trainer;
import com.gymcrm.model.TrainingType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

//single entry in trainee list per REST task 5,6,10,11
//relation fixes hal key when this type is returned as a trainer summary collection model per REST task10,11,
//so it doesn't depend on HATEOAS default reflection names
@Schema(description = "Trainer summary, as shown inside a Trainers List")
@Relation(value = "trainer", collectionRelation = "trainers")
public record TrainerSummary(

        @Schema(description = "Trainer username", example = "marcus.lee")
        String username,

        @Schema(description = "Trainer first name", example = "Marcus")
        String firstName,

        @Schema(description = "Trainer last name", example = "Lee")
        String lastName,

        @Schema(description = "Trainer specialization")
        TrainingType.TrainingTypeName specialization
) {
    public static TrainerSummary from(Trainer trainer) {
        return new TrainerSummary(
                trainer.getUsername(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getSpecialization().getTrainingTypeName());
    }
}
