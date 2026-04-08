package com.claudecode.core.engine;

import java.time.Instant;

/**
 * Task 48.12: Headless profiler checkpoint.
 * Records a point-in-time snapshot during query execution for diagnostics.
 */
public record ProfilerCheckpoint(
    String label,
    Instant timestamp,
    long elapsedMs,
    int messageCount,
    long inputTokens,
    long outputTokens
) {
    public static ProfilerCheckpoint of(String label, long elapsedMs, int messageCount, long inputTokens, long outputTokens) {
        return new ProfilerCheckpoint(label, Instant.now(), elapsedMs, messageCount, inputTokens, outputTokens);
    }
}
