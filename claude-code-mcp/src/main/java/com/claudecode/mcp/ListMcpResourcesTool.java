package com.claudecode.mcp;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool that lists resources available from connected MCP servers.
 * Sends a "resources/list" JSON-RPC request to the specified server.
 */
public class ListMcpResourcesTool extends Tool<JsonNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final McpClientManager clientManager;

    public ListMcpResourcesTool(McpClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public String name() {
        return "mcp__listResources";
    }

    @Override
    public String description() {
        return "Lists resources available from MCP servers";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = MAPPER.createObjectNode();
        ObjectNode serverIdProp = MAPPER.createObjectNode();
        serverIdProp.put("type", "string");
        serverIdProp.put("description", "The MCP server ID to list resources from");
        props.set("serverId", serverIdProp);
        schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("serverId"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String serverId = input.has("serverId") ? input.get("serverId").asText() : "";
        if (serverId.isBlank()) {
            return "Error: serverId is required";
        }

        return clientManager.getConnection(serverId)
            .map(conn -> {
                try {
                    JsonNode result = conn.getTransport().sendRequest("resources/list", null);
                    return result.toString();
                } catch (McpException e) {
                    return "Error listing resources: " + e.getMessage();
                }
            })
            .orElse("Error: no active connection to server '" + serverId + "'");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
