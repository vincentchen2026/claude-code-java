package com.claudecode.tools;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        context = ToolExecutionContext.of(new AbortController(), "test-session");
    }

    @Test
    void nameIsWebSearch() {
        assertEquals("WebSearch", new WebSearchTool().name());
    }

    @Test
    void callWithNoProviderReturnsNotConfigured() {
        WebSearchTool tool = new WebSearchTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("query", "test query");

        String result = tool.call(input, context);
        assertTrue(result.contains("Web search not configured"));
    }

    @Test
    void callWithEmptyQueryReturnsError() {
        WebSearchTool tool = new WebSearchTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("query", "");

        String result = tool.call(input, context);
        assertEquals("Error: query is required", result);
    }

    @Test
    void callWithMissingQueryReturnsError() {
        WebSearchTool tool = new WebSearchTool();
        ObjectNode input = MAPPER.createObjectNode();

        String result = tool.call(input, context);
        assertEquals("Error: query is required", result);
    }

    @Test
    void callWithCustomProvider() {
        WebSearchProvider provider = query -> "Results for: " + query;
        WebSearchTool tool = new WebSearchTool(provider);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("query", "java 21");

        String result = tool.call(input, context);
        assertEquals("Results for: java 21", result);
    }

    @Test
    void callWithProviderException() {
        WebSearchProvider provider = query -> { throw new RuntimeException("API error"); };
        WebSearchTool tool = new WebSearchTool(provider);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("query", "test");

        String result = tool.call(input, context);
        assertTrue(result.startsWith("Error: search failed:"));
    }

    @Test
    void isReadOnly() {
        assertTrue(new WebSearchTool().isReadOnly());
    }

    @Test
    void isConcurrencySafe() {
        assertTrue(new WebSearchTool().isConcurrencySafe());
    }

    @Test
    void schemaHasRequiredFields() {
        WebSearchTool tool = new WebSearchTool();
        var schema = tool.inputSchema();
        assertTrue(schema.has("properties"));
        assertTrue(schema.get("properties").has("query"));
    }
}
