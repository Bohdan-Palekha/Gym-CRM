package com.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

//security task 8
@Schema(description = "Successful login result")
public record LoginResponse(

        @Schema(description = "The authenticated username", example = "alice.nguyen")
        String username,

        @Schema(description = "JWT bearer token. Send as 'Authorization: Bearer <token>' on subsequent requests.")
        String token
) {
}
