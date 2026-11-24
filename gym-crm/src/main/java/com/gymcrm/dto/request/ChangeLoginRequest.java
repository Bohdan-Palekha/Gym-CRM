package com.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

//REST task14
//no username, only passwords, caller identified by auth header
@Schema(description = "Change password request for the currently authenticated user")
public record ChangeLoginRequest(

        @Schema(description = "Current password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "oldPassword is required")
        String oldPassword,

        @Schema(description = "New password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "newPassword is required")
        String newPassword
) {
}

