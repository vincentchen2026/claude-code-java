package com.claudecode.bridge;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PollConfigManager {

    private final Map<String, PollConfig> configs;
    private final Duration defaultPollInterval;
    private final Duration maxPollInterval;

    public PollConfigManager() {
        this(Duration.ofSeconds(5), Duration.ofMinutes(5));
    }

    public PollConfigManager(Duration defaultPollInterval, Duration maxPollInterval) {
        this.configs = new ConcurrentHashMap<>();
        this.defaultPollInterval = defaultPollInterval;
        this.maxPollInterval = maxPollInterval;
    }

    public void setPollConfig(String sessionId, PollConfig config) {
        configs.put(sessionId, config);
    }

    public PollConfig getPollConfig(String sessionId) {
        return configs.getOrDefault(sessionId, PollConfig.DEFAULT);
    }

    public Duration getPollInterval(String sessionId) {
        PollConfig config = getPollConfig(sessionId);
        Duration interval = config.interval();

        if (config.adaptive() && config.failureCount() > 0) {
            long backoffMultiplier = Math.min(1L << config.failureCount(), 8);
            interval = Duration.ofMillis(interval.toMillis() * backoffMultiplier);

            if (interval.compareTo(maxPollInterval) > 0) {
                interval = maxPollInterval;
            }
        }

        return interval;
    }

    public void recordSuccess(String sessionId) {
        PollConfig existing = configs.get(sessionId);
        if (existing != null) {
            configs.put(sessionId, new PollConfig(
                existing.enabled(),
                existing.interval(),
                existing.adaptive(),
                0,
                existing.maxFailures()
            ));
        }
    }

    public void recordFailure(String sessionId) {
        PollConfig existing = configs.get(sessionId);
        if (existing != null) {
            int newFailureCount = existing.failureCount() + 1;
            configs.put(sessionId, new PollConfig(
                existing.enabled(),
                existing.interval(),
                existing.adaptive(),
                newFailureCount,
                existing.maxFailures()
            ));
        }
    }

    public boolean shouldStopPolling(String sessionId) {
        PollConfig config = getPollConfig(sessionId);
        return config.maxFailures() > 0 && config.failureCount() >= config.maxFailures();
    }

    public void removeConfig(String sessionId) {
        configs.remove(sessionId);
    }

    public int getConfigCount() {
        return configs.size();
    }

    public record PollConfig(
        boolean enabled,
        Duration interval,
        boolean adaptive,
        int failureCount,
        int maxFailures
    ) {
        public static final PollConfig DEFAULT = new PollConfig(true, Duration.ofSeconds(5), true, 0, 10);
    }
}