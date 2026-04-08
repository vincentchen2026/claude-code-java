package com.claudecode.services.tasks;

import java.time.Instant;
import java.util.Optional;

/**
 * Full task state record.
 */
public record TaskState(
    String id,
    TaskType type,
    TaskStatus status,
    String description,
    Optional<String> toolUseId,
    Instant startTime,
    Optional<Instant> endTime,
    boolean notified
) {

    public static TaskState create(TaskType type, String description) {
        return new TaskState(
            TaskIdGenerator.generate(type),
            type,
            TaskStatus.PENDING,
            description,
            Optional.empty(),
            Instant.now(),
            Optional.empty(),
            false
        );
    }

    public TaskState withStatus(TaskStatus newStatus) {
        TaskStateMachine.validateTransition(this.status, newStatus);
        Optional<Instant> end = newStatus.isTerminal() ? Optional.of(Instant.now()) : this.endTime;
        return new TaskState(id, type, newStatus, description, toolUseId, startTime, end, notified);
    }

    public TaskState withNotified(boolean notified) {
        return new TaskState(id, type, status, description, toolUseId, startTime, endTime, notified);
    }

    public TaskState withDescription(String description) {
        return new TaskState(id, type, status, description, toolUseId, startTime, endTime, notified);
    }
}
