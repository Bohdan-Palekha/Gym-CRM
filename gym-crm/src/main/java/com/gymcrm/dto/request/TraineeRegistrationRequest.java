package com.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

//REST task 1
//username and password are made on the server
@Schema(description = "Trainee registration request")
public record TraineeRegistrationRequest(

        @Schema(description = "Trainee's first name", example = "Alice", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "firstName is required")
        String firstName,

        @Schema(description = "Trainee's last name", example = "Nguyen", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "lastName is required")
        String lastName,

        @Schema(description = "Date of birth (optional)", example = "1998-05-12")
        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth,

        @Schema(description = "Home address (optional)", example = "12 River Road, Springfield")
        String address
) {
}
