package com.claudecode.services.coordinator;

/**
 * Tracks the current state of a worker agent.
 */
public record WorkerState(
    String workerId,
    Status status,
    int turnsCompleted,
    long contextRemaining,
    double progress
) {
    public enum Status {
        IDLE,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public static WorkerState idle(String workerId) {
        return new WorkerState(workerId, Status.IDLE, 0, 0, 0.0);
    }
}
