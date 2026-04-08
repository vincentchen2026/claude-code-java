package com.claudecode.services.tasks;

/**
 * Task lifecycle status.
 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    KILLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == KILLED;
    }
}
