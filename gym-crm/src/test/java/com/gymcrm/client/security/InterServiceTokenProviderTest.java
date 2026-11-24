package com.gymcrm.client.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InterServiceTokenProvider")
class InterServiceTokenProviderTest {

    private static final String SECRET = "unit-test-inter-service-secret-0123456789-0123456789";

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private final InterServiceTokenProvider provider =
            new InterServiceTokenProvider(SECRET, 300_000L, "gym-crm-service", "trainer-workload-service");

    @Test
    @DisplayName("generates a token signed with the configured secret, carrying scope/target/subject claims")
    void generateToken_hasExpectedClaims() {
        String token = provider.generateToken();

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals("gym-crm-service", claims.getSubject());
        assertEquals("INTERNAL_SERVICE", claims.get("scope", String.class));
        assertEquals("trainer-workload-service", claims.get("targetService", String.class));
        assertNotNull(claims.getId());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    @DisplayName("a token signed with a different secret fails verification")
    void tokenSignedWithWrongSecret_failsVerification() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-0123456789-0123456789".getBytes(StandardCharsets.UTF_8));

        String token = provider.generateToken();

        assertThrows(io.jsonwebtoken.security.SignatureException.class, () ->
                Jwts.parser().verifyWith(wrongKey).build().parseSignedClaims(token));
    }

    @Test
    @DisplayName("expiration reflects the configured TTL")
    void expiration_reflectsConfiguredTtl() {
        InterServiceTokenProvider shortLived =
                new InterServiceTokenProvider(SECRET, 1000L, "gym-crm-service", "trainer-workload-service");

        Instant before = Instant.now();
        String token = shortLived.generateToken();
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        long ttlMs = claims.getExpiration().getTime() - before.toEpochMilli();
        assertTrue(ttlMs <= 1500 && ttlMs > 0, "expiration should be ~1000ms out, was " + ttlMs + "ms");
    }

    @Test
    @DisplayName("uses HS256 regardless of secret length — two independently-configured secrets "
            + "that differ only in length must still land on the same algorithm, or verification "
            + "on the other side fails with a confusing 'key too weak' error instead of working")
    void generateToken_alwaysUsesHs256RegardlessOfSecretLength() {
        String shortSecret = "unit-test-secret-32-bytes-long!!";       //256 bits — the HS256 floor
        String longSecret = "unit-test-secret-that-is-considerably-longer-than-sixty-four-bytes-long-0123456789";

        SecretKey shortKey = Keys.hmacShaKeyFor(shortSecret.getBytes(StandardCharsets.UTF_8));
        SecretKey longKey = Keys.hmacShaKeyFor(longSecret.getBytes(StandardCharsets.UTF_8));

        String shortToken = new InterServiceTokenProvider(
                shortSecret, 300_000L, "gym-crm-service", "trainer-workload-service").generateToken();
        String longToken = new InterServiceTokenProvider(
                longSecret, 300_000L, "gym-crm-service", "trainer-workload-service").generateToken();

        String shortAlg = Jwts.parser().verifyWith(shortKey).build()
                .parseSignedClaims(shortToken).getHeader().getAlgorithm();
        String longAlg = Jwts.parser().verifyWith(longKey).build()
                .parseSignedClaims(longToken).getHeader().getAlgorithm();

        assertEquals("HS256", shortAlg);
        assertEquals("HS256", longAlg);
    }
}

