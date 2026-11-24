package com.gymcrm.workload;

import com.gymcrm.workload.dto.ActionType;
import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.repository.InMemoryRepositoryTestConfig;
import com.gymcrm.workload.service.TrainerWorkloadService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(InMemoryRepositoryTestConfig.class)
@DisplayName("Security — end to end (task 7)")
class SecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private TrainerWorkloadService trainerWorkloadService;

    @Value("${security.jwt.inter-service.secret}")
    private String secret;

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("gym-crm-service")
                .claim("scope", "INTERNAL_SERVICE")
                .claim("targetService", "trainer-workload-service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("no Authorization header -> 401")
    void noToken_returns401() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/trainer-workloads/bob.trainer", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("a garbage bearer token -> 401")
    void garbageToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-real-jwt");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/trainer-workloads/bob.trainer", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("a valid inter-service token -> 200, with the actual recorded summary")
    void validToken_returns200WithSummary() {
        trainerWorkloadService.applyWorkloadChange(new TrainerWorkloadRequest(
                "security-it.trainer", "Sec", "Trainer", true, LocalDate.of(2026, 8, 1), 30, ActionType.ADD));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validToken());

        ResponseEntity<MonthlyTrainingHoursResponse> response = restTemplate.exchange(
                "/api/trainer-workloads/security-it.trainer/years/2026/months/8",
                HttpMethod.GET, new HttpEntity<>(headers), MonthlyTrainingHoursResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(30, response.getBody().trainingSummaryDuration());
    }

    @Test
    @DisplayName("swagger docs remain publicly reachable without a token")
    void swaggerDocs_arePublic() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
