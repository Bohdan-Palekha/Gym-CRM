package com.gymcrm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//security task 7
@DisplayName("JwtBlacklistService")
class JwtBlacklistServiceTest {

    private JwtBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new JwtBlacklistService();
    }

    @Test
    @DisplayName("a token nobody has logged out with is not blacklisted")
    void unknownToken_isNotBlacklisted() {
        assertFalse(service.isBlacklisted("some.jwt.token"));
    }

    @Test
    @DisplayName("a blacklisted token is reported as blacklisted")
    void blacklistedToken_isReported() {
        service.blacklist("some.jwt.token", Instant.now().plusSeconds(3600));
        assertTrue(service.isBlacklisted("some.jwt.token"));
    }

    @Test
    @DisplayName("blacklisting one token does not affect another")
    void blacklistingIsPerToken() {
        service.blacklist("token-a", Instant.now().plusSeconds(3600));
        assertFalse(service.isBlacklisted("token-b"));
    }

    @Test
    @DisplayName("inserting a new entry opportunistically purges already-expired ones")
    void insertPurgesExpiredEntries() {
        service.blacklist("already-expired", Instant.now().minusSeconds(1));
        assertTrue(service.isBlacklisted("already-expired"), "still present immediately after insert");

        service.blacklist("a-fresh-token", Instant.now().plusSeconds(3600));

        assertFalse(service.isBlacklisted("already-expired"), "purged as part of the next insert");
        assertTrue(service.isBlacklisted("a-fresh-token"));
    }
}
