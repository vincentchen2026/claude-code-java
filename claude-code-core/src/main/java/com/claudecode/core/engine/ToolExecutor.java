package com.claudecode.core.engine;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for executing tools requested by the assistant.
 * Implementations handle the actual tool logic (file read, bash, etc.).
 * <p>
 * This will be fully implemented in Task 8 (Tool system).
 * For now, a {@link NoOpToolExecutor} placeholder is provided.
 */
public interface ToolExecutor {

    /**
     * Executes a tool and returns the result.
     */
    ToolResult execute(String toolName, JsonNode input, ToolExecutionContext context);

    /**
     * Returns tool definitions for the API request.
     * Default returns empty list (no tools advertised to the model).
     */
    default java.util.List<StreamingClient.StreamRequest.ToolDef> getToolDefinitions() {
        return java.util.List.of();
    }
}
