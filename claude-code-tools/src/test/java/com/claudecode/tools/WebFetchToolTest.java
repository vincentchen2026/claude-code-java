package com.claudecode.tools;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebFetchToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        context = ToolExecutionContext.of(new AbortController(), "test-session");
    }

    @Test
    void nameIsWebFetch() {
        assertEquals("WebFetch", new WebFetchTool().name());
    }

    @Test
    void callWithEmptyUrlReturnsError() {
        WebFetchTool tool = new WebFetchTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("url", "");

        String result = tool.call(input, context);
        assertEquals("Error: url is required", result);
    }

    @Test
    void callWithMissingUrlReturnsError() {
        WebFetchTool tool = new WebFetchTool();
        ObjectNode input = MAPPER.createObjectNode();

        String result = tool.call(input, context);
        assertEquals("Error: url is required", result);
    }

    @Test
    void callWithInvalidSchemeReturnsError() {
        WebFetchTool tool = new WebFetchTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("url", "ftp://example.com");

        String result = tool.call(input, context);
        assertEquals("Error: only http and https URLs are supported", result);
    }

    @Test
    void callWithInvalidUrlReturnsError() {
        WebFetchTool tool = new WebFetchTool();
        ObjectNode input = MAPPER.createObjectNode();
        input.put("url", "not a url at all %%");

        String result = tool.call(input, context);
        assertTrue(result.startsWith("Error:"));
    }

    @Test
    void htmlToTextStripsBasicTags() {
        String html = "<html><body><h1>Hello</h1><p>World</p></body></html>";
        String text = WebFetchTool.htmlToText(html);
        assertTrue(text.contains("Hello"));
        assertTrue(text.contains("World"));
        assertFalse(text.contains("<h1>"));
        assertFalse(text.contains("<p>"));
    }

    @Test
    void htmlToTextRemovesScriptAndStyle() {
        String html = "<html><head><style>body{color:red}</style></head>"
            + "<body><script>alert('x')</script><p>Content</p></body></html>";
        String text = WebFetchTool.htmlToText(html);
        assertTrue(text.contains("Content"));
        assertFalse(text.contains("alert"));
        assertFalse(text.contains("color:red"));
    }

    @Test
    void htmlToTextDecodesEntities() {
        String html = "<p>A &amp; B &lt; C &gt; D &quot;E&quot; F&#39;s</p>";
        String text = WebFetchTool.htmlToText(html);
        assertTrue(text.contains("A & B"));
        assertTrue(text.contains("< C >"));
        assertTrue(text.contains("\"E\""));
        assertTrue(text.contains("F's"));
    }

    @Test
    void htmlToTextCollapsesWhitespace() {
        String html = "<p>Hello     World</p>";
        String text = WebFetchTool.htmlToText(html);
        assertFalse(text.contains("     "));
    }

    @Test
    void isReadOnly() {
        assertTrue(new WebFetchTool().isReadOnly());
    }

    @Test
    void isConcurrencySafe() {
        assertTrue(new WebFetchTool().isConcurrencySafe());
    }

    @Test
    void schemaHasRequiredFields() {
        WebFetchTool tool = new WebFetchTool();
        var schema = tool.inputSchema();
        assertTrue(schema.has("properties"));
        assertTrue(schema.get("properties").has("url"));
        assertTrue(schema.get("properties").has("timeout"));
    }
}
