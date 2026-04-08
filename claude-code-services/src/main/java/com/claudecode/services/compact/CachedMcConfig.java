package com.claudecode.services.compact;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedMcConfig {

    private final Map<String, McConfigEntry> cache;
    private final Duration ttl;
    private final int maxSize;

    public CachedMcConfig() {
        this(Duration.ofMinutes(5), 1000);
    }

    public CachedMcConfig(Duration ttl, int maxSize) {
        this.cache = new ConcurrentHashMap<>();
        this.ttl = ttl;
        this.maxSize = maxSize;
    }

    public void put(String sessionId, TimeBasedMcConfig.McConfig config) {
        if (cache.size() >= maxSize) {
            evictStale();
        }
        cache.put(sessionId, new McConfigEntry(config, Instant.now().plus(ttl)));
    }

    public TimeBasedMcConfig.McConfig get(String sessionId) {
        McConfigEntry entry = cache.get(sessionId);
        if (entry == null) {
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(sessionId);
            return null;
        }

        return entry.config();
    }

    public TimeBasedMcConfig.McConfig getOrCompute(String sessionId, java.util.function.Supplier<TimeBasedMcConfig.McConfig> computer) {
        TimeBasedMcConfig.McConfig existing = get(sessionId);
        if (existing != null) {
            return existing;
        }

        TimeBasedMcConfig.McConfig computed = computer.get();
        if (computed != null) {
            put(sessionId, computed);
        }
        return computed;
    }

    public void invalidate(String sessionId) {
        cache.remove(sessionId);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public long size() {
        return cache.size();
    }

    private void evictStale() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    public record McConfigEntry(
        TimeBasedMcConfig.McConfig config,
        Instant expiresAt
    ) {}
}