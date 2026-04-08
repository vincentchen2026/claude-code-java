package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages connections to MCP servers.
 * Handles connecting, disconnecting, tool discovery, and tool invocation.
 */
public class McpClientManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(McpClientManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, McpConnection> connections = new ConcurrentHashMap<>();

    /**
     * Connects to an MCP server using the given configuration.
     * If a connection with the same server name already exists, it is replaced.
     *
     * @param config the server configuration
     * @return the established connection
     */
    public McpConnection connect(McpServerConfig config) {
        if (config.disabled()) {
            throw new McpException("Cannot connect to disabled server '" + config.name() + "'");
        }

        // Disconnect existing connection if present
        disconnect(config.name());

        McpTransport transport = createTransport(config);
        McpConnection connection = new McpConnection(config, transport);
        connections.put(config.name(), connection);

        LOG.info("Connected to MCP server '{}'", config.name());
        return connection;
    }

    /**
     * Disconnects from the specified MCP server.
     *
     * @param serverId the server identifier
     */
    public void disconnect(String serverId) {
        McpConnection conn = connections.remove(serverId);
        if (conn != null) {
            try {
                conn.close();
                LOG.info("Disconnected from MCP server '{}'", serverId);
            } catch (Exception e) {
                LOG.warn("Error disconnecting from MCP server '{}'", serverId, e);
            }
        }
    }

    /**
     * Returns the connection for the specified server, if it exists and is connected.
     */
    public Optional<McpConnection> getConnection(String serverId) {
        McpConnection conn = connections.get(serverId);
        if (conn != null && conn.isConnected()) {
            return Optional.of(conn);
        }
        return Optional.empty();
    }

    /**
     * Discovers tools from all connected MCP servers.
     * Sends a "tools/list" JSON-RPC request to each server.
     *
     * @return aggregated list of tools from all servers
     */
    public List<McpToolInfo> listTools() {
        List<McpToolInfo> allTools = new ArrayList<>();
        for (McpConnection conn : connections.values()) {
            if (!conn.isConnected()) continue;
            try {
                List<McpToolInfo> tools = discoverTools(conn);
                conn.setCachedTools(tools);
                allTools.addAll(tools);
            } catch (McpException e) {
                LOG.warn("Failed to list tools from server '{}'", conn.getServerId(), e);
            }
        }
        return Collections.unmodifiableList(allTools);
    }

    /**
     * Discovers tools from a specific MCP server.
     *
     * @param serverId the server identifier
     * @return list of tools from that server
     */
    public List<McpToolInfo> listToolsForServer(String serverId) {
        McpConnection conn = connections.get(serverId);
        if (conn == null || !conn.isConnected()) {
            return List.of();
        }
        try {
            List<McpToolInfo> tools = discoverTools(conn);
            conn.setCachedTools(tools);
            return tools;
        } catch (McpException e) {
            LOG.warn("Failed to list tools from server '{}'", serverId, e);
            return List.of();
        }
    }

    /**
     * Invokes a tool on the specified MCP server.
     *
     * @param serverId the server to call
     * @param toolName the tool to invoke
     * @param args     the tool arguments
     * @return the tool result as JSON
     */
    public JsonNode callTool(String serverId, String toolName, JsonNode args) {
        McpConnection conn = connections.get(serverId);
        if (conn == null || !conn.isConnected()) {
            throw new McpException("No active connection to server '" + serverId + "'");
        }

        var params = MAPPER.createObjectNode();
        params.put("name", toolName);
        if (args != null) {
            params.set("arguments", args);
        }

        return conn.getTransport().sendRequest("tools/call", params);
    }

    /**
     * Returns all active server IDs.
     */
    public Set<String> getConnectedServerIds() {
        Set<String> ids = new HashSet<>();
        for (var entry : connections.entrySet()) {
            if (entry.getValue().isConnected()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    @Override
    public void close() throws Exception {
        for (String serverId : new ArrayList<>(connections.keySet())) {
            disconnect(serverId);
        }
    }

    // -- internal --

    private McpTransport createTransport(McpServerConfig config) {
        return switch (config.transportType()) {
            case "stdio" -> {
                StdioTransport t = new StdioTransport(config);
                t.start();
                yield t;
            }
            case "sse" -> {
                SseTransport t = new SseTransport(config.command());
                t.connect();
                yield t;
            }
            default -> throw new McpException("Unknown transport type: " + config.transportType());
        };
    }

    private List<McpToolInfo> discoverTools(McpConnection conn) {
        JsonNode result = conn.getTransport().sendRequest("tools/list", null);
        List<McpToolInfo> tools = new ArrayList<>();

        JsonNode toolsNode = result.get("tools");
        if (toolsNode != null && toolsNode.isArray()) {
            for (JsonNode toolNode : toolsNode) {
                String name = toolNode.has("name") ? toolNode.get("name").asText() : "";
                String desc = toolNode.has("description") ? toolNode.get("description").asText() : "";
                JsonNode schema = toolNode.has("inputSchema") ? toolNode.get("inputSchema") : MAPPER.createObjectNode();
                tools.add(new McpToolInfo(conn.getServerId(), name, desc, schema));
            }
        }
        return tools;
    }
}
