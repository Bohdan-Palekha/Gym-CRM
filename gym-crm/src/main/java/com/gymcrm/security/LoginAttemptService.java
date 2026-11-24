package com.gymcrm.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//security task 6
//brute force protector
//in-memory, username failed counter
//max attempts set to 3
//lockout duration set to 5
//success clears the counter
//clock is injected for tests, so there is no need for sleeping
@Slf4j
@Component
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockoutDuration;
    private final Clock clock;

    private final ConcurrentMap<String, Attempt> attemptsByUsername = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${security.brute-force.max-attempts:3}") int maxAttempts,
            @Value("${security.brute-force.lockout-duration-minutes:5}") long lockoutDurationMinutes) {
        this(maxAttempts, Duration.ofMinutes(lockoutDurationMinutes), Clock.systemUTC());
    }

    //clock injection for tests
    LoginAttemptService(int maxAttempts, Duration lockoutDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockoutDuration = lockoutDuration;
        this.clock = clock;
    }

    public void loginSucceeded(String username) {
        if (attemptsByUsername.remove(username) != null) {
            log.debug("Cleared failed-login counter for username={}", username);
        }
    }

    public void loginFailed(String username) {
        Attempt updated = attemptsByUsername.compute(username, (key, existing) -> {
            int count = (existing == null ? 0 : existing.count()) + 1;
            Instant blockedUntil = count >= maxAttempts ? Instant.now(clock).plus(lockoutDuration) : null;
            return new Attempt(count, blockedUntil);
        });
        if (updated.blockedUntil() != null) {
            log.warn("username={} locked for {} after {} consecutive failed login attempts",
                    username, lockoutDuration, updated.count());
        }
    }

    public boolean isBlocked(String username) {
        Attempt attempt = attemptsByUsername.get(username);
        if (attempt == null || attempt.blockedUntil() == null) {
            return false;
        }
        if (!Instant.now(clock).isBefore(attempt.blockedUntil())) {
            attemptsByUsername.remove(username);
            return false;
        }
        return true;
    }

    private record Attempt(int count, Instant blockedUntil) {
    }
}
