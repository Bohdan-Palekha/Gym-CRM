package com.gymcrm.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "unit-test-signing-secret-at-least-32-bytes-long-0123456789";
    private static final String OTHER_SECRET = "a-completely-different-signing-secret-0123456789-abcdefghij";

    private JwtService jwtService;
    private UserDetails aliceDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
        aliceDetails = User.withUsername("alice.nguyen").password("irrelevant").authorities("ROLE_TRAINEE").build();
    }

    @Test
    @DisplayName("generated token's subject round-trips through extractUsername")
    void generateThenExtractUsername_roundTrips() {
        String token = jwtService.generateToken("alice.nguyen");
        assertEquals("alice.nguyen", jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("a freshly generated token is valid for the matching user")
    void freshToken_isValidForMatchingUser() {
        String token = jwtService.generateToken("alice.nguyen");
        assertTrue(jwtService.isTokenValid(token, aliceDetails));
    }

    @Test
    @DisplayName("a token is not valid for a different username")
    void token_isNotValidForDifferentUser() {
        String token = jwtService.generateToken("alice.nguyen");
        UserDetails bobDetails = User.withUsername("bob.jones").password("x").authorities("ROLE_TRAINER").build();
        assertFalse(jwtService.isTokenValid(token, bobDetails));
    }

    @Test
    @DisplayName("extractExpiration returns a time after issuance, matching the configured TTL")
    void extractExpiration_matchesConfiguredTtl() {
        Instant before = Instant.now();
        String token = jwtService.generateToken("alice.nguyen");
        Instant expiration = jwtService.extractExpiration(token);

        assertTrue(expiration.isAfter(before));
        assertTrue(expiration.isBefore(before.plusSeconds(120)));
    }

    @Test
    @DisplayName("an expired token fails validation with a JwtException")
    void expiredToken_throws() {
        JwtService shortLivedIssuer = new JwtService(SECRET, -10_000); // already expired the moment it's minted
        String expiredToken = shortLivedIssuer.generateToken("alice.nguyen");

        assertThrows(JwtException.class, () -> jwtService.isTokenValid(expiredToken, aliceDetails));
        assertThrows(JwtException.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    @DisplayName("a token signed with a different secret fails signature validation")
    void wrongSigningKey_throws() {
        JwtService otherIssuer = new JwtService(OTHER_SECRET, 60_000);
        String tokenFromOtherIssuer = otherIssuer.generateToken("alice.nguyen");

        assertThrows(JwtException.class, () -> jwtService.extractUsername(tokenFromOtherIssuer));
    }

    @Test
    @DisplayName("a malformed token string throws rather than returning a null/empty username")
    void malformedToken_throws() {
        assertThrows(JwtException.class, () -> jwtService.extractUsername("not.a.jwt"));
    }

    @Test
    @DisplayName("a blank token string throws IllegalArgumentException")
    void blankToken_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractUsername(""));
    }
}
