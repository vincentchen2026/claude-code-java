package com.claudecode.core.engine;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Placeholder tool executor that returns a "not yet implemented" result.
 * Will be replaced by the real ToolExecutor in Task 8.
 */
public class NoOpToolExecutor implements ToolExecutor {

    @Override
    public ToolResult execute(String toolName, JsonNode input, ToolExecutionContext context) {
        return ToolResult.success("Tool '" + toolName + "' not yet implemented");
    }
}
