package com.lanre.personl.iso20022.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    @Test
    @DisplayName("Should evict requests outside the active rate-limit window")
    void shouldEvictOldRequestsOutsideWindow() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-04-16T00:00:00Z"));
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(new MutableClock(now));

        assertTrue(rateLimiter.allow("127.0.0.1", 2, 60));
        assertTrue(rateLimiter.allow("127.0.0.1", 2, 60));
        assertFalse(rateLimiter.allow("127.0.0.1", 2, 60));

        now.set(now.get().plusSeconds(61));

        assertTrue(rateLimiter.allow("127.0.0.1", 2, 60));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(AtomicReference<Instant> current) {
            this.current = current;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
