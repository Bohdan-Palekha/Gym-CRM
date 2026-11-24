package com.gymcrm.dto.request;

import com.gymcrm.model.TrainingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//REST task 2
//username and password are made on the server
@Schema(description = "Trainer registration request")
public record TrainerRegistrationRequest(

        @Schema(description = "Trainer's first name", example = "Marcus", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "firstName is required")
        String firstName,

        @Schema(description = "Trainer's last name", example = "Lee", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "lastName is required")
        String lastName,

        @Schema(description = "Training type this trainer specializes in", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "specialization is required")
        TrainingType.TrainingTypeName specialization
) {
}
