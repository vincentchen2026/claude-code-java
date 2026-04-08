package com.claudecode.mcp;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a single MCP server.
 *
 * @param name          server identifier
 * @param command       executable command to start the server
 * @param args          command-line arguments
 * @param env           environment variables for the server process
 * @param disabled      whether this server is disabled
 * @param autoApprove   list of tool names that are auto-approved
 * @param transportType "stdio" or "sse"
 */
public record McpServerConfig(
    String name,
    String command,
    List<String> args,
    Map<String, String> env,
    boolean disabled,
    List<String> autoApprove,
    String transportType
) {
    public McpServerConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Server name must not be blank");
        }
        if (args == null) args = List.of();
        if (env == null) env = Map.of();
        if (autoApprove == null) autoApprove = List.of();
        if (transportType == null || transportType.isBlank()) transportType = "stdio";
    }
}
