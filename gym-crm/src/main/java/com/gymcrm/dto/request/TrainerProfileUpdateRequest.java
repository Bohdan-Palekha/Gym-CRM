package com.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//REST task 9
//specialization is read-only, service never writes to it
@Schema(description = "Trainer profile update request")
public record TrainerProfileUpdateRequest(

        @Schema(description = "First name", example = "Marcus", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "firstName is required")
        String firstName,

        @Schema(description = "Last name", example = "Lee", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "lastName is required")
        String lastName,

        @JsonProperty("isActive")
        @Schema(description = "Whether the trainer profile is active", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}
