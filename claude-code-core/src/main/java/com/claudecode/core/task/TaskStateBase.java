package com.claudecode.core.task;

/**
 * Placeholder record for task state.
 * Will be fully implemented in Task 29 (後台任務系統).
 *
 * @param id          unique task identifier
 * @param type        task type string (e.g., "local_bash", "local_agent")
 * @param status      task status string (e.g., "pending", "running", "completed")
 * @param description human-readable task description
 */
public record TaskStateBase(
    String id,
    String type,
    String status,
    String description
) {}
