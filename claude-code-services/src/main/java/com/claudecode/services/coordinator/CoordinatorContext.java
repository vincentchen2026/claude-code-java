package com.claudecode.services.coordinator;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Context provided to the coordinator for prompt generation.
 */
public record CoordinatorContext(
    List<String> availableTools,
    List<String> mcpServers,
    Path scratchpadDir,
    int maxWorkers,
    Map<String, String> extraContext
) {
    public CoordinatorContext {
        if (availableTools == null) availableTools = List.of();
        if (mcpServers == null) mcpServers = List.of();
        if (extraContext == null) extraContext = Map.of();
        if (maxWorkers < 1) maxWorkers = 4;
    }
}
