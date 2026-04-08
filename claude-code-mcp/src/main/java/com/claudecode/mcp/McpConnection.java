package com.claudecode.mcp;

import java.util.List;

/**
 * Represents an active connection to an MCP server.
 * Wraps the transport and caches discovered tools.
 */
public class McpConnection implements AutoCloseable {

    private final McpServerConfig config;
    private final McpTransport transport;
    private volatile List<McpToolInfo> cachedTools;

    public McpConnection(McpServerConfig config, McpTransport transport) {
        this.config = config;
        this.transport = transport;
    }

    public McpServerConfig getConfig() {
        return config;
    }

    public McpTransport getTransport() {
        return transport;
    }

    public String getServerId() {
        return config.name();
    }

    public List<McpToolInfo> getCachedTools() {
        return cachedTools;
    }

    public void setCachedTools(List<McpToolInfo> tools) {
        this.cachedTools = tools;
    }

    public boolean isConnected() {
        return transport.isConnected();
    }

    @Override
    public void close() throws Exception {
        transport.close();
    }
}
