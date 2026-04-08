package com.claudecode.services.compact;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeBasedMcConfig {

    private final Map<String, ConfigEntry> configs;
    private final Duration defaultTtl;

    public TimeBasedMcConfig() {
        this(Duration.ofHours(1));
    }

    public TimeBasedMcConfig(Duration defaultTtl) {
        this.configs = new ConcurrentHashMap<>();
        this.defaultTtl = defaultTtl;
    }

    public void setConfig(String key, McConfig value) {
        setConfig(key, value, defaultTtl);
    }

    public void setConfig(String key, McConfig value, Duration ttl) {
        configs.put(key, new ConfigEntry(value, Instant.now().plus(ttl)));
    }

    public McConfig getConfig(String key) {
        ConfigEntry entry = configs.get(key);
        if (entry == null) {
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            configs.remove(key);
            return null;
        }

        return entry.config();
    }

    public boolean isExpired(String key) {
        ConfigEntry entry = configs.get(key);
        if (entry == null) {
            return true;
        }
        return Instant.now().isAfter(entry.expiresAt());
    }

    public void invalidate(String key) {
        configs.remove(key);
    }

    public void invalidateExpired() {
        Instant now = Instant.now();
        configs.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    public void clear() {
        configs.clear();
    }

    public int size() {
        return configs.size();
    }

    public record ConfigEntry(
        McConfig config,
        Instant expiresAt
    ) {}

    public record McConfig(
        String sessionId,
        int maxMessages,
        int maxTokens,
        boolean enableSnipProjection,
        boolean enableCompactPrompt
    ) {}
}