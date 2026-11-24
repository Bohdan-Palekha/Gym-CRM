package com.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;


//REST task 15,16
//by REST note 9 not idempotent
//if sent desired state is identical to the current one, gives 409
//according to criterion calling twice does not give same effect
@Schema(description = "Activate/deactivate request")
public record ActiveStatusRequest(

        @JsonProperty("isActive")
        @Schema(description = "Desired active state", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}
