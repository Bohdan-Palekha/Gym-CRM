package com.gymcrm.workload.security;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServiceTokenValidator")
class ServiceTokenValidatorTest {

    private static final String SECRET = "unit-test-inter-service-secret-0123456789-0123456789";

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private final ServiceTokenValidator validator =
            new ServiceTokenValidator(SECRET, "trainer-workload-service");

    private String token(String scope, String target, long ttlMs, String subject) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)));
        if (scope != null) {
            builder = builder.claim("scope", scope);
        }
        if (target != null) {
            builder = builder.claim("targetService", target);
        }
        return builder.signWith(key).compact();
    }

    @Test
    @DisplayName("a correctly scoped, correctly targeted, validly signed token is accepted")
    void validToken_isAccepted() {
        String token = token("INTERNAL_SERVICE", "trainer-workload-service", 60_000, "gym-crm-service");
        assertEquals("gym-crm-service", validator.validate(token).orElseThrow());
    }

    @Test
    @DisplayName("a token missing the targetService claim is still accepted (lenient by design)")
    void missingTargetClaim_isAccepted() {
        String token = token("INTERNAL_SERVICE", null, 60_000, "gym-crm-service");
        assertTrue(validator.validate(token).isPresent());
    }

    @Test
    @DisplayName("a token targeted at a different service is rejected")
    void wrongTarget_isRejected() {
        String token = token("INTERNAL_SERVICE", "some-other-service", 60_000, "gym-crm-service");
        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    @DisplayName("a token with the wrong scope is rejected")
    void wrongScope_isRejected() {
        String token = token("SOMETHING_ELSE", "trainer-workload-service", 60_000, "gym-crm-service");
        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    @DisplayName("a token missing the scope claim entirely is rejected")
    void missingScope_isRejected() {
        String token = token(null, "trainer-workload-service", 60_000, "gym-crm-service");
        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredToken_isRejected() {
        String token = token("INTERNAL_SERVICE", "trainer-workload-service", -1000, "gym-crm-service");
        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void wrongSignature_isRejected() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a-totally-different-secret-0123456789-0123456789".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("gym-crm-service")
                .claim("scope", "INTERNAL_SERVICE")
                .claim("targetService", "trainer-workload-service")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(wrongKey)
                .compact();

        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    @DisplayName("garbage input is rejected, not thrown")
    void garbageToken_isRejected() {
        assertTrue(validator.validate("not-a-real-jwt").isEmpty());
    }
}
