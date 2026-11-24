package com.gymcrm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//datasource are supplied, test is self-contained
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:gymcrm_actuator_it;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.hikari.maximum-pool-size=3",
                "hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "hibernate.hbm2ddl.auto=create-drop",
                "hibernate.show_sql=false",
                "hibernate.format_sql=false",
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
                "management.endpoint.health.show-details=always",
                "management.prometheus.metrics.export.enabled=true",
                "info.app.name=Gym Application Test",
                "info.app.description=Gym CRM REST API test instance",
                "management.info.env.enabled=true"
        })
@DisplayName("Actuator wiring (tasks 2-4)")
class ActuatorIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /actuator/info returns the configured app info, not a blank {}")
    void infoEndpoint_returnsConfiguredAppInfo() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        // management.info.env.enabled must stay on, or info.app.* will stip showing
        assertTrue(response.getBody().contains("Gym Application Test"));
    }

    @Test
    @DisplayName("GET /actuator/health is publicly reachable, UP, and lists both custom indicators")
    void healthEndpoint_isUpAndListsCustomIndicators() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
        //custom actuators
        assertTrue(response.getBody().contains("database"));
        assertTrue(response.getBody().contains("trainingTypeCatalog"));
    }

    @Test
    @DisplayName("GET /actuator/prometheus is publicly reachable and exposes the custom metrics")
    void prometheusEndpoint_exposesCustomMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        //2 custom metrics in prometheus
        assertTrue(response.getBody().contains("gym_registrations_total"));
        assertTrue(response.getBody().contains("gym_authentication_attempts_total"));
    }

    @Test
    @DisplayName("actuator requires no Authorization header, unlike the business /api endpoints")
    void actuatorEndpoints_doNotRequireBasicAuth() {
        ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(200, health.getStatusCode().value());

        //check if auth works at all
        ResponseEntity<String> trainee = restTemplate.getForEntity("/api/trainees/no-such-user", String.class);
        assertEquals(401, trainee.getStatusCode().value());
    }
}
