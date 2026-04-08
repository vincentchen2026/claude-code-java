package com.claudecode.services.tasks;

import java.util.Map;
import java.util.Set;

/**
 * Validates task state transitions.
 * Enforces: pending → running → completed/failed/killed
 *           pending → killed (direct cancel)
 *           running → killed (abort)
 */
public final class TaskStateMachine {

    private TaskStateMachine() {}

    private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS = Map.of(
        TaskStatus.PENDING, Set.of(TaskStatus.RUNNING, TaskStatus.KILLED),
        TaskStatus.RUNNING, Set.of(TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.KILLED),
        TaskStatus.COMPLETED, Set.of(),
        TaskStatus.FAILED, Set.of(),
        TaskStatus.KILLED, Set.of()
    );

    /**
     * Returns whether transitioning from {@code from} to {@code to} is valid.
     */
    public static boolean isValidTransition(TaskStatus from, TaskStatus to) {
        Set<TaskStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Validates a transition, throwing if invalid.
     */
    public static void validateTransition(TaskStatus from, TaskStatus to) {
        if (!isValidTransition(from, to)) {
            throw new IllegalStateException(
                "Invalid task state transition: " + from + " → " + to);
        }
    }
}
