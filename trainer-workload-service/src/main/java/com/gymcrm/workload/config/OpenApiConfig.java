package com.gymcrm.workload.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI trainerWorkloadOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trainer Workload Service API")
                        .description("Secondary microservice for gym-crm: consumes trainer workload events "
                                + "(training planned/cancelled) over ActiveMQ and serves back a trainer's monthly "
                                + "training-hours summary over REST. The read endpoints below require a "
                                + "service-to-service JWT bearer token (scope=INTERNAL_SERVICE), sent as "
                                + "'Authorization: Bearer <token>'.")
                        .version("1.0.0")
                        .contact(new Contact().name("Gym CRM")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Inter-service JWT bearer token (scope=INTERNAL_SERVICE).")));
    }
}
