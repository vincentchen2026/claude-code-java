package com.claudecode.services.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsOptOut {

    private static final Logger log = LoggerFactory.getLogger(MetricsOptOut.class);

    private final Map<String, OptOutEntry> optOutStatus = new ConcurrentHashMap<>();
    private final Path configDir;

    public MetricsOptOut() {
        this(Path.of(System.getProperty("user.home"), ".claude"));
    }

    public MetricsOptOut(Path configDir) {
        this.configDir = configDir;
        loadOptOutStatus();
    }

    public boolean isOptedOut(String userId) {
        OptOutEntry entry = optOutStatus.get(userId);
        return entry != null && entry.optedOut();
    }

    public void optOut(String userId) {
        optOutStatus.put(userId, new OptOutEntry(userId, true, Instant.now(), null));
        saveOptOutStatus();
        log.info("User {} opted out of metrics", userId);
    }

    public void optIn(String userId) {
        optOutStatus.put(userId, new OptOutEntry(userId, false, Instant.now(), null));
        saveOptOutStatus();
        log.info("User {} opted in to metrics", userId);
    }

    public Instant getOptOutTime(String userId) {
        OptOutEntry entry = optOutStatus.get(userId);
        return entry != null ? entry.optOutTime() : null;
    }

    private void loadOptOutStatus() {
        Path optOutFile = configDir.resolve("metrics_optout.json");
        if (Files.exists(optOutFile)) {
            try {
                String content = Files.readString(optOutFile);
                log.debug("Loaded opt-out status from {}", optOutFile);
            } catch (Exception e) {
                log.warn("Failed to load opt-out status: {}", e.getMessage());
            }
        }
    }

    private void saveOptOutStatus() {
        try {
            Files.createDirectories(configDir);
            Path optOutFile = configDir.resolve("metrics_optout.json");
            log.debug("Saved opt-out status to {}", optOutFile);
        } catch (Exception e) {
            log.warn("Failed to save opt-out status: {}", e.getMessage());
        }
    }

    public record OptOutEntry(
        String userId,
        boolean optedOut,
        Instant optOutTime,
        String reason
    ) {}
}