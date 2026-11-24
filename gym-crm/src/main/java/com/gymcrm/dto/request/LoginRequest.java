package com.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

//security tasks 3,5
//credentials travel in json over POST, never land in URL, server access log, browser history
@Schema(description = "Login request")
public record LoginRequest(

        @Schema(description = "Trainee's or trainer's username", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "username is required")
        String username,

        @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "password is required")
        String password
) {
}
