package com.claudecode.tools;

import java.util.Optional;

/**
 * Result of a sub-agent execution.
 *
 * @param output           the text output from the sub-agent
 * @param tokensUsed       total tokens consumed by the sub-agent
 * @param costUsd          estimated USD cost of the sub-agent run
 * @param taskId           optional task ID if run as background task
 * @param worktreePath     optional git worktree path if isolation was used
 * @param error            optional error message if execution failed
 */
public record SubAgentResult(
    String output,
    long tokensUsed,
    double costUsd,
    Optional<String> taskId,
    Optional<String> worktreePath,
    Optional<String> error
) {

    public static SubAgentResult of(String output) {
        return new SubAgentResult(output, 0, 0.0, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static SubAgentResult of(String output, long tokensUsed, double costUsd) {
        return new SubAgentResult(output, tokensUsed, costUsd, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static SubAgentResult error(String message) {
        return new SubAgentResult("Error: " + message, 0, 0.0, Optional.empty(), Optional.empty(), Optional.of(message));
    }

    public static SubAgentResult withTask(String output, String taskId) {
        return new SubAgentResult(output, 0, 0.0, Optional.of(taskId), Optional.empty(), Optional.empty());
    }

    public static SubAgentResult withWorktree(String output, String worktreePath) {
        return new SubAgentResult(output, 0, 0.0, Optional.empty(), Optional.of(worktreePath), Optional.empty());
    }

    public boolean isError() {
        return error.isPresent();
    }

    public String output() {
        if (error.isPresent()) {
            return "Error: " + error.get();
        }
        return output;
    }
}
