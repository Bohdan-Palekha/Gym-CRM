package com.gymcrm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

//security task 6
//mutable clock is used instead of thread.sleep
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new LoginAttemptService(3, Duration.ofMinutes(5), clock);
    }

    @Test
    @DisplayName("a username with no attempts is never blocked")
    void neverAttempted_notBlocked() {
        assertFalse(service.isBlocked("carol"));
    }

    @Test
    @DisplayName("fewer than 3 consecutive failures do not block")
    void belowThreshold_notBlocked() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        assertFalse(service.isBlocked("alice"));
    }

    @Test
    @DisplayName("the 3rd consecutive failure blocks the username")
    void thirdFailure_blocks() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");
        assertTrue(service.isBlocked("alice"));
    }

    @Test
    @DisplayName("stays blocked for the full 5 minutes")
    void staysBlockedUntilWindowElapses() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");

        clock.advance(Duration.ofMinutes(5).minusMillis(1));
        assertTrue(service.isBlocked("alice"), "still within the 5-minute window");
    }

    @Test
    @DisplayName("unblocks itself once 5 minutes have fully elapsed")
    void unblocksAfterWindowElapses() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");

        clock.advance(Duration.ofMinutes(5).plusMillis(1));
        assertFalse(service.isBlocked("alice"));
    }

    @Test
    @DisplayName("a successful login clears the failure counter")
    void successResetsCounter() {
        service.loginFailed("dave");
        service.loginFailed("dave");
        service.loginSucceeded("dave");

        service.loginFailed("dave");
        assertFalse(service.isBlocked("dave"), "only 1 failure since the reset");

        service.loginFailed("dave");
        service.loginFailed("dave");
        assertTrue(service.isBlocked("dave"), "3 fresh failures after the reset do block");
    }

    @Test
    @DisplayName("blocking one username does not affect another")
    void blockingIsPerUsername() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");

        assertTrue(service.isBlocked("alice"));
        assertFalse(service.isBlocked("bob"));
    }

    @Test
    @DisplayName("concurrent failures for the same username still correctly reach blocked")
    void concurrentFailures_stillBlockCorrectly() throws InterruptedException {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        LoginAttemptService realTimeService = new LoginAttemptService(3, Duration.ofMinutes(5), Clock.systemUTC());
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                realTimeService.loginFailed("eve");
            });
        }
        ready.await(2, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(realTimeService.isBlocked("eve"));
    }
}
