package com.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

//REST task 1,2 / security task 8
@Schema(description = "Generated credentials for a newly registered trainee or trainer, "
        + "plus a bearer token for the new account (already logged in)")
public record RegistrationResponse(

        @Schema(description = "Generated, unique username", example = "alice.nguyen")
        String username,

        @Schema(description = "Generated password", example = "a1B2c3D4")
        String password,

        @Schema(description = "JWT bearer token for the newly created account. "
                + "Send as 'Authorization: Bearer <token>' on subsequent requests.")
        String token
) {
}
