package com.claudecode.services.coordinator;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for a worker agent.
 */
public record WorkerConfig(
    String workerId,
    String taskDescription,
    WorkerMode mode,
    Optional<String> model,
    List<String> allowedTools,
    int maxTurns,
    double maxBudgetUsd
) {
    public WorkerConfig {
        if (allowedTools == null) allowedTools = List.of();
        if (maxTurns < 1) maxTurns = 50;
        if (maxBudgetUsd <= 0) maxBudgetUsd = 1.0;
    }

    public enum WorkerMode {
        /** Bash/Read/Edit only */
        SIMPLE,
        /** All tools available */
        FULL
    }
}
