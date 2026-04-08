package com.claudecode.mcp;

import java.util.List;
import java.util.Map;

/**
 * Aggregated MCP configuration after merging all levels.
 *
 * @param servers map of server name to server configuration
 */
public record McpConfig(
    Map<String, McpServerConfig> servers
) {
    public McpConfig {
        if (servers == null) servers = Map.of();
    }

    /**
     * Returns all enabled server configurations.
     */
    public List<McpServerConfig> enabledServers() {
        return servers.values().stream()
            .filter(s -> !s.disabled())
            .toList();
    }
}
