package com.claudecode.tools;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        context = ToolExecutionContext.of(new AbortController(), "test-session");
    }

    @Test
    void nameIsAgent() {
        AgentTool tool = new AgentTool();
        assertEquals("Agent", tool.name());
    }

    @Test
    void callWithNoOpFactory() {
        AgentTool tool = new AgentTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "List all files");

        String result = tool.call(input, context);
        assertTrue(result.contains("Sub-agent not configured"));
        assertTrue(result.contains("List all files"));
    }

    @Test
    void callWithEmptyPromptReturnsError() {
        AgentTool tool = new AgentTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "");

        String result = tool.call(input, context);
        assertEquals("Error: prompt is required", result);
    }

    @Test
    void callWithMissingPromptReturnsError() {
        AgentTool tool = new AgentTool();
        ObjectNode input = MAPPER.createObjectNode();

        String result = tool.call(input, context);
        assertEquals("Error: prompt is required", result);
    }

    @Test
    void callWithCustomFactory() {
        SubAgentFactory factory = request -> SubAgentResult.of("Custom result for: " + request.prompt());
        AgentTool tool = new AgentTool(factory);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "Do something");

        String result = tool.call(input, context);
        assertEquals("Custom result for: Do something", result);
    }

    @Test
    void callWithToolListRestriction() {
        SubAgentFactory factory = request -> {
            assertEquals(List.of("Bash", "FileRead"), request.tools());
            return SubAgentResult.of("ok");
        };
        AgentTool tool = new AgentTool(factory);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "test");
        input.putArray("tools").add("Bash").add("FileRead");

        tool.call(input, context);
    }

    @Test
    void callWithDefaultToolSet() {
        SubAgentFactory factory = request -> {
            assertEquals(AgentTool.DEFAULT_SAFE_TOOLS, request.tools());
            return SubAgentResult.of("ok");
        };
        AgentTool tool = new AgentTool(factory);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "test");

        tool.call(input, context);
    }

    @Test
    void callWithBudgetAllocation() {
        SubAgentFactory factory = request -> {
            assertEquals(0.5, request.budgetUsd(), 0.001);
            return SubAgentResult.of("ok");
        };
        AgentTool tool = new AgentTool(factory);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "test");
        input.put("budget_usd", 0.5);

        tool.call(input, context);
    }

    @Test
    void callWithDefaultBudget() {
        SubAgentFactory factory = request -> {
            assertEquals(AgentTool.BUDGET_FRACTION, request.budgetUsd(), 0.001);
            return SubAgentResult.of("ok");
        };
        AgentTool tool = new AgentTool(factory);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "test");

        tool.call(input, context);
    }

    @Test
    void callWithFactoryException() {
        SubAgentFactory factory = request -> { throw new RuntimeException("boom"); };
        AgentTool tool = new AgentTool(factory);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("prompt", "test");

        String result = tool.call(input, context);
        assertTrue(result.startsWith("Error: sub-agent execution failed:"));
    }

    @Test
    void schemaHasRequiredFields() {
        AgentTool tool = new AgentTool();
        var schema = tool.inputSchema();
        assertTrue(schema.has("properties"));
        assertTrue(schema.get("properties").has("prompt"));
        assertTrue(schema.get("properties").has("tools"));
        assertTrue(schema.get("properties").has("budget_usd"));
    }

    @Test
    void isNotReadOnly() {
        assertFalse(new AgentTool().isReadOnly());
    }
}
