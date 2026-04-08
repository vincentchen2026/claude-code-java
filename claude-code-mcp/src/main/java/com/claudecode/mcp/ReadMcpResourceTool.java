package com.claudecode.mcp;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool that reads a specific resource from an MCP server.
 * Sends a "resources/read" JSON-RPC request with the resource URI.
 */
public class ReadMcpResourceTool extends Tool<JsonNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final McpClientManager clientManager;

    public ReadMcpResourceTool(McpClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public String name() {
        return "mcp__readResource";
    }

    @Override
    public String description() {
        return "Reads a specific resource from an MCP server";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = MAPPER.createObjectNode();

        ObjectNode serverIdProp = MAPPER.createObjectNode();
        serverIdProp.put("type", "string");
        serverIdProp.put("description", "The MCP server ID");
        props.set("serverId", serverIdProp);

        ObjectNode uriProp = MAPPER.createObjectNode();
        uriProp.put("type", "string");
        uriProp.put("description", "The resource URI to read");
        props.set("uri", uriProp);

        schema.set("properties", props);
        ArrayNode required = MAPPER.createArrayNode();
        required.add("serverId");
        required.add("uri");
        schema.set("required", required);
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String serverId = input.has("serverId") ? input.get("serverId").asText() : "";
        String uri = input.has("uri") ? input.get("uri").asText() : "";

        if (serverId.isBlank() || uri.isBlank()) {
            return "Error: serverId and uri are required";
        }

        return clientManager.getConnection(serverId)
            .map(conn -> {
                try {
                    ObjectNode params = MAPPER.createObjectNode();
                    params.put("uri", uri);
                    JsonNode result = conn.getTransport().sendRequest("resources/read", params);
                    return result.toString();
                } catch (McpException e) {
                    return "Error reading resource: " + e.getMessage();
                }
            })
            .orElse("Error: no active connection to server '" + serverId + "'");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
