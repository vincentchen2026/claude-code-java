package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StdioTransport} using a simple echo-like process.
 * Uses a bash script that reads a line from stdin and echoes back a JSON-RPC response.
 */
class StdioTransportTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void sendRequestAndReceiveResponse() throws Exception {
        // Use bash to create a simple JSON-RPC echo server:
        // reads one line, extracts the id, returns a response with that id
        McpServerConfig config = new McpServerConfig(
            "echo-server",
            "bash",
            List.of("-c", "while IFS= read -r line; do " +
                "id=$(echo \"$line\" | sed 's/.*\"id\":\\([0-9]*\\).*/\\1/'); " +
                "echo \"{\\\"jsonrpc\\\":\\\"2.0\\\",\\\"id\\\":$id,\\\"result\\\":{\\\"echo\\\":true}}\"; " +
                "done"),
            Map.of(),
            false,
            List.of(),
            "stdio"
        );

        StdioTransport transport = new StdioTransport(config);
        try {
            transport.start();
            assertTrue(transport.isConnected());

            ObjectNode params = MAPPER.createObjectNode();
            params.put("test", "value");

            JsonNode result = transport.sendRequest("test/method", params);
            assertNotNull(result);
            assertTrue(result.has("echo"));
            assertTrue(result.get("echo").asBoolean());
        } finally {
            transport.close();
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void multipleRequests() throws Exception {
        McpServerConfig config = new McpServerConfig(
            "multi-echo",
            "bash",
            List.of("-c", "while IFS= read -r line; do " +
                "id=$(echo \"$line\" | sed 's/.*\"id\":\\([0-9]*\\).*/\\1/'); " +
                "echo \"{\\\"jsonrpc\\\":\\\"2.0\\\",\\\"id\\\":$id,\\\"result\\\":{\\\"count\\\":$id}}\"; " +
                "done"),
            Map.of(),
            false,
            List.of(),
            "stdio"
        );

        StdioTransport transport = new StdioTransport(config);
        try {
            transport.start();

            JsonNode r1 = transport.sendRequest("method1", null);
            JsonNode r2 = transport.sendRequest("method2", null);

            assertNotNull(r1);
            assertNotNull(r2);
            // IDs are sequential: 1, 2
            assertEquals(1, r1.get("count").asInt());
            assertEquals(2, r2.get("count").asInt());
        } finally {
            transport.close();
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void closeStopsProcess() throws Exception {
        McpServerConfig config = new McpServerConfig(
            "close-test",
            "bash",
            List.of("-c", "while IFS= read -r line; do echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}'; done"),
            Map.of(),
            false,
            List.of(),
            "stdio"
        );

        StdioTransport transport = new StdioTransport(config);
        transport.start();
        assertTrue(transport.isConnected());

        transport.close();
        assertFalse(transport.isConnected());
    }

    @Test
    void sendRequestBeforeStartThrows() {
        McpServerConfig config = new McpServerConfig(
            "not-started", "echo", List.of(), Map.of(), false, List.of(), "stdio");

        StdioTransport transport = new StdioTransport(config);
        // Not started, so not connected
        assertThrows(McpException.class, () -> transport.sendRequest("test", null));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void errorResponseThrows() throws Exception {
        // Server that always returns an error
        McpServerConfig config = new McpServerConfig(
            "error-server",
            "bash",
            List.of("-c", "while IFS= read -r line; do " +
                "id=$(echo \"$line\" | sed 's/.*\"id\":\\([0-9]*\\).*/\\1/'); " +
                "echo \"{\\\"jsonrpc\\\":\\\"2.0\\\",\\\"id\\\":$id,\\\"error\\\":{\\\"code\\\":-1,\\\"message\\\":\\\"test error\\\"}}\"; " +
                "done"),
            Map.of(),
            false,
            List.of(),
            "stdio"
        );

        StdioTransport transport = new StdioTransport(config);
        try {
            transport.start();
            McpException ex = assertThrows(McpException.class,
                () -> transport.sendRequest("fail", null));
            assertTrue(ex.getMessage().contains("test error"));
        } finally {
            transport.close();
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void environmentVariablesArePassed() throws Exception {
        // Server that echoes an env var in the response
        McpServerConfig config = new McpServerConfig(
            "env-server",
            "bash",
            List.of("-c", "while IFS= read -r line; do " +
                "id=$(echo \"$line\" | sed 's/.*\"id\":\\([0-9]*\\).*/\\1/'); " +
                "echo \"{\\\"jsonrpc\\\":\\\"2.0\\\",\\\"id\\\":$id,\\\"result\\\":{\\\"val\\\":\\\"$MCP_TEST_VAR\\\"}}\"; " +
                "done"),
            Map.of("MCP_TEST_VAR", "hello123"),
            false,
            List.of(),
            "stdio"
        );

        StdioTransport transport = new StdioTransport(config);
        try {
            transport.start();
            JsonNode result = transport.sendRequest("env/check", null);
            assertEquals("hello123", result.get("val").asText());
        } finally {
            transport.close();
        }
    }
}
