package com.claudecode.api;

import com.claudecode.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CreateMessageRequest serialization and builder.
 */
class CreateMessageRequestTest {

    @Test
    void builderCreatesValidRequest() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .model("claude-sonnet-4-20250514")
                .maxTokens(1024)
                .systemPrompt("You are a helpful assistant.")
                .messages(List.of(
                        new CreateMessageRequest.RequestMessage("user", "Hello")))
                .stream(true)
                .build();

        assertEquals("claude-sonnet-4-20250514", request.model());
        assertEquals(1024, request.maxTokens());
        assertEquals("You are a helpful assistant.", request.systemPrompt());
        assertEquals(1, request.messages().size());
        assertTrue(request.stream());
    }

    @Test
    void serializesToCorrectJsonFormat() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .model("claude-sonnet-4-20250514")
                .maxTokens(4096)
                .systemPrompt("System prompt")
                .messages(List.of(
                        new CreateMessageRequest.RequestMessage("user", "Hello, Claude!")))
                .stream(true)
                .build();

        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);

        assertEquals("claude-sonnet-4-20250514", node.get("model").asText());
        assertEquals(4096, node.get("max_tokens").asInt());
        assertEquals("System prompt", node.get("system").asText());
        assertTrue(node.get("stream").asBoolean());
        assertTrue(node.get("messages").isArray());
        assertEquals(1, node.get("messages").size());
        assertEquals("user", node.get("messages").get(0).get("role").asText());
        assertEquals("Hello, Claude!", node.get("messages").get(0).get("content").asText());
    }

    @Test
    void nullFieldsAreOmittedInJson() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .model("claude-sonnet-4-20250514")
                .maxTokens(1024)
                .messages(List.of())
                .stream(true)
                .build();

        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);

        // Null fields should not appear
        assertFalse(node.has("system") && !node.get("system").isNull(),
                "system should be null or absent");
        assertFalse(node.has("tools") && !node.get("tools").isNull(),
                "tools should be null or absent");
        assertFalse(node.has("temperature") && !node.get("temperature").isNull(),
                "temperature should be null or absent");
    }

    @Test
    void toolDefinitionSerializesCorrectly() {
        JsonNode schema = JsonUtils.parseTree("""
                {"type": "object", "properties": {"command": {"type": "string"}}}
                """);

        CreateMessageRequest.ToolDefinition tool = new CreateMessageRequest.ToolDefinition(
                "bash", "Execute a shell command", schema);

        CreateMessageRequest request = CreateMessageRequest.builder()
                .model("claude-sonnet-4-20250514")
                .maxTokens(1024)
                .messages(List.of())
                .tools(List.of(tool))
                .stream(true)
                .build();

        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);

        assertTrue(node.has("tools"));
        assertEquals(1, node.get("tools").size());
        JsonNode toolNode = node.get("tools").get(0);
        assertEquals("bash", toolNode.get("name").asText());
        assertEquals("Execute a shell command", toolNode.get("description").asText());
        assertTrue(toolNode.has("input_schema"));
    }

    @Test
    void builderDefaultValues() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .model("test-model")
                .build();

        assertEquals(4096, request.maxTokens());
        assertTrue(request.stream());
        assertNotNull(request.messages());
    }

    @Test
    void multipleMessagesSerializeCorrectly() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .model("claude-sonnet-4-20250514")
                .maxTokens(1024)
                .messages(List.of(
                        new CreateMessageRequest.RequestMessage("user", "Hello"),
                        new CreateMessageRequest.RequestMessage("assistant", "Hi there!"),
                        new CreateMessageRequest.RequestMessage("user", "How are you?")))
                .stream(true)
                .build();

        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);

        assertEquals(3, node.get("messages").size());
        assertEquals("user", node.get("messages").get(0).get("role").asText());
        assertEquals("assistant", node.get("messages").get(1).get("role").asText());
        assertEquals("user", node.get("messages").get(2).get("role").asText());
    }
}
