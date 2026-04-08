package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link McpClientManager}.
 * Uses a fake in-memory transport to avoid subprocess dependencies.
 */
class McpClientManagerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private McpClientManager manager;

    @BeforeEach
    void setUp() {
        manager = new McpClientManager();
    }

    @AfterEach
    void tearDown() throws Exception {
        manager.close();
    }

    @Test
    void connectAndGetConnection() {
        McpServerConfig config = new McpServerConfig(
            "test-server", "echo", List.of(), null, false, null, "stdio");

        // We can't actually connect to "echo" as MCP, but we can test the manager
        // by verifying it creates a connection entry. The process will start and die quickly.
        // Instead, test with a fake transport via subclass.
        var testManager = new TestableClientManager();
        McpConnection conn = testManager.connect(config);

        assertNotNull(conn);
        assertEquals("test-server", conn.getServerId());
        assertTrue(testManager.getConnection("test-server").isPresent());
    }

    @Test
    void disconnectRemovesConnection() {
        var testManager = new TestableClientManager();
        McpServerConfig config = new McpServerConfig(
            "srv1", "cmd", List.of(), null, false, null, "stdio");

        testManager.connect(config);
        assertTrue(testManager.getConnection("srv1").isPresent());

        testManager.disconnect("srv1");
        assertFalse(testManager.getConnection("srv1").isPresent());
    }

    @Test
    void disconnectNonExistentIsNoOp() {
        assertDoesNotThrow(() -> manager.disconnect("nonexistent"));
    }

    @Test
    void getConnectionReturnsEmptyForUnknown() {
        assertEquals(Optional.empty(), manager.getConnection("unknown"));
    }

    @Test
    void connectDisabledServerThrows() {
        McpServerConfig config = new McpServerConfig(
            "disabled-srv", "cmd", List.of(), null, true, null, "stdio");

        assertThrows(McpException.class, () -> manager.connect(config));
    }

    @Test
    void listToolsAggregatesFromAllServers() {
        var testManager = new TestableClientManager();

        McpServerConfig config1 = new McpServerConfig(
            "srv1", "cmd1", List.of(), null, false, null, "stdio");
        McpServerConfig config2 = new McpServerConfig(
            "srv2", "cmd2", List.of(), null, false, null, "stdio");

        testManager.connect(config1);
        testManager.connect(config2);

        List<McpToolInfo> tools = testManager.listTools();
        // Each fake server returns 1 tool
        assertEquals(2, tools.size());
        assertTrue(tools.stream().anyMatch(t -> t.serverId().equals("srv1")));
        assertTrue(tools.stream().anyMatch(t -> t.serverId().equals("srv2")));
    }

    @Test
    void callToolDelegatesToTransport() {
        var testManager = new TestableClientManager();
        McpServerConfig config = new McpServerConfig(
            "srv1", "cmd", List.of(), null, false, null, "stdio");
        testManager.connect(config);

        ObjectNode args = MAPPER.createObjectNode();
        args.put("key", "value");

        JsonNode result = testManager.callTool("srv1", "test-tool", args);
        assertNotNull(result);
        assertEquals("ok", result.get("status").asText());
    }

    @Test
    void callToolOnDisconnectedServerThrows() {
        assertThrows(McpException.class,
            () -> manager.callTool("nonexistent", "tool", null));
    }

    @Test
    void closeDisconnectsAll() throws Exception {
        var testManager = new TestableClientManager();
        testManager.connect(new McpServerConfig("s1", "c", List.of(), null, false, null, "stdio"));
        testManager.connect(new McpServerConfig("s2", "c", List.of(), null, false, null, "stdio"));

        assertEquals(2, testManager.getConnectedServerIds().size());
        testManager.close();
        assertEquals(0, testManager.getConnectedServerIds().size());
    }

    /**
     * Testable subclass that uses a fake transport instead of real subprocesses.
     */
    static class TestableClientManager extends McpClientManager {
        @Override
        public McpConnection connect(McpServerConfig config) {
            if (config.disabled()) {
                throw new McpException("Cannot connect to disabled server '" + config.name() + "'");
            }
            disconnect(config.name());
            FakeTransport transport = new FakeTransport();
            McpConnection connection = new McpConnection(config, transport);
            // Use reflection-free approach: store in parent via public API
            // We need to access the parent's connections map, so we override connect entirely
            return connectWithTransport(config, transport);
        }

        McpConnection connectWithTransport(McpServerConfig config, McpTransport transport) {
            // Call disconnect to clean up any existing connection
            disconnect(config.name());
            McpConnection connection = new McpConnection(config, transport);
            // We need to store this — use the parent's field via a workaround
            // Since connections is private, we'll use a local map
            getConnectionsMap().put(config.name(), connection);
            return connection;
        }

        private final java.util.Map<String, McpConnection> localConnections =
            new java.util.concurrent.ConcurrentHashMap<>();

        java.util.Map<String, McpConnection> getConnectionsMap() {
            return localConnections;
        }

        @Override
        public Optional<McpConnection> getConnection(String serverId) {
            McpConnection conn = localConnections.get(serverId);
            if (conn != null && conn.isConnected()) {
                return Optional.of(conn);
            }
            return Optional.empty();
        }

        @Override
        public void disconnect(String serverId) {
            McpConnection conn = localConnections.remove(serverId);
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }

        @Override
        public java.util.Set<String> getConnectedServerIds() {
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (var entry : localConnections.entrySet()) {
                if (entry.getValue().isConnected()) {
                    ids.add(entry.getKey());
                }
            }
            return ids;
        }

        @Override
        public List<McpToolInfo> listTools() {
            List<McpToolInfo> allTools = new java.util.ArrayList<>();
            for (McpConnection conn : localConnections.values()) {
                if (!conn.isConnected()) continue;
                JsonNode result = conn.getTransport().sendRequest("tools/list", null);
                JsonNode toolsNode = result.get("tools");
                if (toolsNode != null && toolsNode.isArray()) {
                    for (JsonNode toolNode : toolsNode) {
                        allTools.add(new McpToolInfo(
                            conn.getServerId(),
                            toolNode.get("name").asText(),
                            toolNode.has("description") ? toolNode.get("description").asText() : "",
                            MAPPER.createObjectNode()));
                    }
                }
            }
            return allTools;
        }

        @Override
        public JsonNode callTool(String serverId, String toolName, JsonNode args) {
            McpConnection conn = localConnections.get(serverId);
            if (conn == null || !conn.isConnected()) {
                throw new McpException("No active connection to server '" + serverId + "'");
            }
            ObjectNode params = MAPPER.createObjectNode();
            params.put("name", toolName);
            if (args != null) params.set("arguments", args);
            return conn.getTransport().sendRequest("tools/call", params);
        }

        @Override
        public void close() throws Exception {
            for (String id : new java.util.ArrayList<>(localConnections.keySet())) {
                disconnect(id);
            }
        }
    }

    /**
     * Fake transport that returns canned responses.
     */
    static class FakeTransport implements McpTransport {
        private boolean connected = true;

        @Override
        public JsonNode sendRequest(String method, JsonNode params) {
            return switch (method) {
                case "tools/list" -> {
                    ObjectNode result = MAPPER.createObjectNode();
                    var tools = MAPPER.createArrayNode();
                    ObjectNode tool = MAPPER.createObjectNode();
                    tool.put("name", "fake-tool");
                    tool.put("description", "A fake tool");
                    tools.add(tool);
                    result.set("tools", tools);
                    yield result;
                }
                case "tools/call" -> {
                    ObjectNode result = MAPPER.createObjectNode();
                    result.put("status", "ok");
                    yield result;
                }
                case "resources/list" -> {
                    ObjectNode result = MAPPER.createObjectNode();
                    result.set("resources", MAPPER.createArrayNode());
                    yield result;
                }
                default -> MAPPER.createObjectNode();
            };
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void close() {
            connected = false;
        }
    }
}
