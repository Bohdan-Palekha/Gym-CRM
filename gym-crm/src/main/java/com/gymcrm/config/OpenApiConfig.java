package com.gymcrm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//bean for metadata and JWT bearer auth scheme for filter
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI gymCrmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM API")
                        .description("Trainee/Trainer registration, profile management and training scheduling. "
                                + "All endpoints except registration (POST /api/trainees, POST /api/trainers) and "
                                + "POST /api/auth/login require a JWT bearer token, obtained from registration or "
                                + "login, sent as 'Authorization: Bearer <token>'.")
                        .version("1.0.0")
                        .contact(new Contact().name("Gym CRM")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT bearer token for an existing trainee's or trainer's account.")));
    }
}
