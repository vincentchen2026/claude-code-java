package com.claudecode.services.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitManager {

    private static final Logger log = LoggerFactory.getLogger(RateLimitManager.class);

    private final Map<String, RateLimitEntry> limits = new ConcurrentHashMap<>();
    private final boolean mockMode;

    public RateLimitManager() {
        this(false);
    }

    public RateLimitManager(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        if (mockMode) {
            return true;
        }

        RateLimitEntry entry = limits.computeIfAbsent(key, k -> new RateLimitEntry(
            key,
            new AtomicInteger(0),
            Instant.now(),
            windowSeconds
        ));

        synchronized (entry) {
            Instant windowStart = entry.windowStart();
            Instant now = Instant.now();

            if (ChronoUnit.SECONDS.between(windowStart, now) >= entry.windowSeconds()) {
                entry.count().set(0);
                entry.windowStart(Instant.now());
            }

            int current = entry.count().incrementAndGet();
            if (current > maxRequests) {
                entry.count().decrementAndGet();
                log.debug("Rate limit exceeded for key: {}", key);
                return false;
            }

            return true;
        }
    }

    public RateLimitStatus getStatus(String key) {
        RateLimitEntry entry = limits.get(key);
        if (entry == null) {
            return new RateLimitStatus(0, 0, true);
        }

        synchronized (entry) {
            int current = entry.count().get();
            int windowSeconds = entry.windowSeconds();
            Instant windowStart = entry.windowStart();
            Instant now = Instant.now();

            long secondsInWindow = ChronoUnit.SECONDS.between(windowStart, now);
            int remainingInWindow = Math.max(0, windowSeconds - (int) secondsInWindow);

            return new RateLimitStatus(current, remainingInWindow, true);
        }
    }

    public void reset(String key) {
        RateLimitEntry entry = limits.get(key);
        if (entry != null) {
            synchronized (entry) {
                entry.count().set(0);
                entry.windowStart(Instant.now());
            }
        }
    }

    public void resetAll() {
        for (RateLimitEntry entry : limits.values()) {
            synchronized (entry) {
                entry.count().set(0);
                entry.windowStart(Instant.now());
            }
        }
        log.info("Reset all rate limits");
    }

    public boolean isMockMode() {
        return mockMode;
    }

    private static class RateLimitEntry {
        private final String key;
        private final AtomicInteger count;
        private volatile Instant windowStart;
        private final int windowSeconds;

        RateLimitEntry(String key, AtomicInteger count, Instant windowStart, int windowSeconds) {
            this.key = key;
            this.count = count;
            this.windowStart = windowStart;
            this.windowSeconds = windowSeconds;
        }

        public String key() { return key; }
        public AtomicInteger count() { return count; }
        public Instant windowStart() { return windowStart; }
        public void windowStart(Instant ws) { this.windowStart = ws; }
        public int windowSeconds() { return windowSeconds; }
    }

    public record RateLimitStatus(
        int currentCount,
        int retryAfterSeconds,
        boolean allowed
    ) {}
}