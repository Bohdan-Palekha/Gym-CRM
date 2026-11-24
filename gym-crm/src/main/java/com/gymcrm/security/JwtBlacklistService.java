package com.gymcrm.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//security task 7
//logout for stateless  JWT setup
//delists token string until natural expiration
//every insert opportunistically purges expired entries
@Slf4j
@Component
public class JwtBlacklistService {

    private final ConcurrentMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklist(String token, Instant expiresAt) {
        purgeExpired();
        blacklistedTokens.put(token, expiresAt);
        log.debug("Token blacklisted (logout), expires at {}", expiresAt);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        blacklistedTokens.values().removeIf(expiresAt -> expiresAt.isBefore(now));
    }
}
