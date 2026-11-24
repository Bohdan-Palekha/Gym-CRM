package com.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

//REST task 6
//username id required in request field
//but according to REST note 7 username is unchangable
// so username is taken from /api/trainees/username path variable instead of being duplicated in body
@Schema(description = "Trainee profile update request")
public record TraineeProfileUpdateRequest(

        @Schema(description = "First name", example = "Alice", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "firstName is required")
        String firstName,

        @Schema(description = "Last name", example = "Nguyen", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "lastName is required")
        String lastName,

        @Schema(description = "Date of birth (optional)", example = "1998-05-12")
        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth,

        @Schema(description = "Home address (optional)", example = "12 River Road, Springfield")
        String address,

        @JsonProperty("isActive")
        @Schema(description = "Whether the trainee profile is active", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}
