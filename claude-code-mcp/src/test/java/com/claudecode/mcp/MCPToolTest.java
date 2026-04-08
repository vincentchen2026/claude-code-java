package com.claudecode.mcp;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MCPTool}.
 */
class MCPToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private McpClientManagerTest.TestableClientManager clientManager;
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        clientManager = new McpClientManagerTest.TestableClientManager();
        McpServerConfig config = new McpServerConfig(
            "test-srv", "cmd", java.util.List.of(), null, false, null, "stdio");
        clientManager.connect(config);

        context = ToolExecutionContext.of(
            new AbortController(), "test-session");
    }

    @Test
    void nameIncludesServerAndToolName() {
        McpToolInfo info = new McpToolInfo("my-server", "my-tool", "desc", MAPPER.createObjectNode());
        MCPTool tool = new MCPTool(info, clientManager);
        assertEquals("mcp__my-server__my-tool", tool.name());
    }

    @Test
    void descriptionFromToolInfo() {
        McpToolInfo info = new McpToolInfo("srv", "tool", "A great tool", MAPPER.createObjectNode());
        MCPTool tool = new MCPTool(info, clientManager);
        assertEquals("A great tool", tool.description());
    }

    @Test
    void inputSchemaFromToolInfo() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        McpToolInfo info = new McpToolInfo("srv", "tool", "desc", schema);
        MCPTool tool = new MCPTool(info, clientManager);
        assertEquals(schema, tool.inputSchema());
    }

    @Test
    void callDelegatesToClientManager() {
        McpToolInfo info = new McpToolInfo("test-srv", "fake-tool", "desc", MAPPER.createObjectNode());
        MCPTool tool = new MCPTool(info, clientManager);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("arg1", "value1");

        String result = tool.call(input, context);
        assertNotNull(result);
        // The fake transport returns {"status":"ok"} for tools/call
        assertTrue(result.contains("ok"));
    }

    @Test
    void callReturnsErrorMessageOnFailure() {
        // Use a tool info pointing to a non-existent server
        McpToolInfo info = new McpToolInfo("nonexistent", "tool", "desc", MAPPER.createObjectNode());
        MCPTool tool = new MCPTool(info, clientManager);

        String result = tool.call(MAPPER.createObjectNode(), context);
        assertTrue(result.startsWith("Error calling MCP tool:"));
    }

    @Test
    void isConcurrencySafe() {
        McpToolInfo info = new McpToolInfo("srv", "tool", "desc", MAPPER.createObjectNode());
        MCPTool tool = new MCPTool(info, clientManager);
        assertTrue(tool.isConcurrencySafe());
    }

    @Test
    void getToolInfoReturnsOriginal() {
        McpToolInfo info = new McpToolInfo("srv", "tool", "desc", MAPPER.createObjectNode());
        MCPTool tool = new MCPTool(info, clientManager);
        assertSame(info, tool.getToolInfo());
    }
}
