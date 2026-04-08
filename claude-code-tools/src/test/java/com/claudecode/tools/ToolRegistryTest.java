package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.core.engine.ToolResult;
import com.claudecode.core.engine.AbortController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    private ToolRegistry registry;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
    }

    @Test
    void registerAndGetTool() {
        Tool<JsonNode, String> tool = new ToolBuilder<JsonNode, String>()
                .name("test-tool")
                .description("A test tool")
                .call((input, ctx) -> "result")
                .build();

        registry.register(tool);

        assertTrue(registry.get("test-tool").isPresent());
        assertEquals("test-tool", registry.get("test-tool").get().name());
    }

    @Test
    void getUnknownToolReturnsEmpty() {
        assertTrue(registry.get("nonexistent").isEmpty());
    }

    @Test
    void getAllReturnsAllRegistered() {
        registry.register(new BashTool());
        registry.register(new FileReadTool());
        assertEquals(2, registry.getAll().size());
    }

    @Test
    void filterByReadOnly() {
        registry.register(new BashTool());
        registry.register(new FileReadTool());
        registry.register(new GrepTool());

        var readOnlyTools = registry.filter(Tool::isReadOnly);
        assertEquals(2, readOnlyTools.size());
    }

    @Test
    void executeUnknownToolReturnsError() {
        ObjectNode input = mapper.createObjectNode();
        ToolExecutionContext ctx = ToolExecutionContext.of(new AbortController(), "test-session");

        ToolResult result = registry.execute("unknown", input, ctx);
        assertTrue(result.isError());
    }

    @Test
    void executeRegisteredTool() {
        registry.register(new FileReadTool());
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "nonexistent-file.txt");

        ToolExecutionContext ctx = ToolExecutionContext.of(new AbortController(), "test-session");
        ToolResult result = registry.execute("Read", input, ctx);
        // Should return error since file doesn't exist, but not a "Unknown tool" error
        assertFalse(result.content().isEmpty());
    }

    @Test
    void registerReplacesExisting() {
        Tool<JsonNode, String> tool1 = new ToolBuilder<JsonNode, String>()
                .name("test")
                .description("first")
                .call((input, ctx) -> "first")
                .build();
        Tool<JsonNode, String> tool2 = new ToolBuilder<JsonNode, String>()
                .name("test")
                .description("second")
                .call((input, ctx) -> "second")
                .build();

        registry.register(tool1);
        registry.register(tool2);

        assertEquals(1, registry.size());
        assertEquals("second", registry.get("test").get().description());
    }
}
