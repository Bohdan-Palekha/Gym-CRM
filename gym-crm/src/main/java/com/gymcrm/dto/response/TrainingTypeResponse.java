package com.gymcrm.dto.response;

import com.gymcrm.model.TrainingType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

//REST task 17
//relation fixes hal key when this type is returned as a training type response collection,
@Schema(description = "Training type reference entry")
@Relation(value = "trainingType", collectionRelation = "trainingTypes")
public record TrainingTypeResponse(

        @Schema(description = "Training type name", example = "YOGA")
        String trainingType,

        @Schema(description = "Training type id", example = "2")
        Long trainingTypeId
) {
    public static TrainingTypeResponse from(TrainingType type) {
        return new TrainingTypeResponse(type.getTrainingTypeName().name(), type.getId());
    }
}
