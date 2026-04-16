package com.lanre.personl.iso20022.api.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryRateLimiter {

    private final ConcurrentMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allow(String key, int maxRequests, long windowSeconds) {
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(windowSeconds);

        Deque<Instant> requests = requestLog.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (requests) {
            while (!requests.isEmpty() && requests.peekFirst().isBefore(cutoff)) {
                requests.removeFirst();
            }

            if (requests.size() >= maxRequests) {
                return false;
            }

            requests.addLast(now);
            return true;
        }
    }

    void clear() {
        requestLog.clear();
    }
}
