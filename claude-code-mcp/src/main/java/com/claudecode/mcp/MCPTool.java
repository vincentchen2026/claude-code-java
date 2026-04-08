package com.claudecode.mcp;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Proxy tool that exposes an MCP server tool as a built-in tool.
 * Delegates execution to {@link McpClientManager#callTool}.
 * The input schema is dynamically obtained from MCP tool discovery.
 */
public class MCPTool extends Tool<JsonNode, String> {

    private final McpToolInfo toolInfo;
    private final McpClientManager clientManager;

    public MCPTool(McpToolInfo toolInfo, McpClientManager clientManager) {
        this.toolInfo = toolInfo;
        this.clientManager = clientManager;
    }

    @Override
    public String name() {
        return "mcp__" + toolInfo.serverId() + "__" + toolInfo.name();
    }

    @Override
    public String description() {
        return toolInfo.description();
    }

    @Override
    public JsonNode inputSchema() {
        return toolInfo.inputSchema();
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        try {
            JsonNode result = clientManager.callTool(
                toolInfo.serverId(), toolInfo.name(), input);
            return result != null ? result.toString() : "";
        } catch (McpException e) {
            return "Error calling MCP tool: " + e.getMessage();
        }
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    /**
     * Returns the underlying MCP tool info.
     */
    public McpToolInfo getToolInfo() {
        return toolInfo;
    }
}
