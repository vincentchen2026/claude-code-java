package com.claudecode.services.coordinator;

import java.time.Duration;
import java.util.Optional;

/**
 * Result from a worker agent execution.
 */
public record WorkerResult(
    String workerId,
    WorkerStatus status,
    Optional<String> output,
    Optional<String> error,
    int turnsUsed,
    Duration elapsed
) {
    public enum WorkerStatus {
        COMPLETED,
        FAILED,
        TIMED_OUT,
        CANCELLED
    }
}
