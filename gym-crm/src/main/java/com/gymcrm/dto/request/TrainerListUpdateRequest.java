package com.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

//REST task 11
//trainee identified by /api/trainees/username/trainers, body has a full replacement list of trainers for trainee assigment
@Schema(description = "Replacement list of trainer usernames for a trainee")
public record TrainerListUpdateRequest(

        @Schema(description = "Full replacement list of trainer usernames", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "trainerUsernames must contain at least one username")
        List<@NotBlank(message = "trainerUsernames must not contain blank entries") String> trainerUsernames
) {
}
