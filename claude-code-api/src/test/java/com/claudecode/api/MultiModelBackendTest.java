package com.claudecode.api;

import com.claudecode.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-model backend clients, prompt caching, and thinking mode.
 */
class MultiModelBackendTest {

    // --- OpenAiCompatClient ---

    @Test
    void openAiCompatClientReturnsModel() {
        var config = new ApiConfig.OpenAiConfig("sk-test", "gpt-4", "https://api.openai.com/v1");
        var client = new OpenAiCompatClient(config);
        assertEquals("gpt-4", client.getModel());
        assertEquals("https://api.openai.com/v1", client.getBaseUrl());
    }

    @Test
    void openAiCompatClientCreateMessage() {
        var config = new ApiConfig.OpenAiConfig("sk-test", "gpt-4", "https://api.openai.com/v1");
        var client = new OpenAiCompatClient(config);
        var request = CreateMessageRequest.builder().model("gpt-4").build();

        ApiMessage response = client.createMessage(request);
        assertNotNull(response);
        assertEquals("gpt-4", response.model());
    }

    @Test
    void openAiCompatClientStreamReturnsEvents() {
        var config = new ApiConfig.OpenAiConfig("sk-test", "gpt-4", "https://api.openai.com/v1");
        var client = new OpenAiCompatClient(config);
        var request = CreateMessageRequest.builder().model("gpt-4").build();

        Iterator<StreamEvent> stream = client.createMessageStream(request);
        assertTrue(stream.hasNext());
        assertInstanceOf(StreamEvent.MessageStart.class, stream.next());
    }

    // --- BedrockClient ---

    @Test
    void bedrockClientReturnsModel() {
        var config = new ApiConfig.BedrockConfig("us-east-1", "anthropic.claude-v2");
        var client = new BedrockClient(config);
        assertEquals("anthropic.claude-v2", client.getModel());
        assertEquals("us-east-1", client.getRegion());
    }

    @Test
    void bedrockClientCreateMessage() {
        var config = new ApiConfig.BedrockConfig("us-east-1", "anthropic.claude-v2");
        var client = new BedrockClient(config);
        var request = CreateMessageRequest.builder().model("anthropic.claude-v2").build();

        ApiMessage response = client.createMessage(request);
        assertNotNull(response);
    }

    @Test
    void bedrockClientStreamReturnsEvents() {
        var config = new ApiConfig.BedrockConfig("us-east-1", "anthropic.claude-v2");
        var client = new BedrockClient(config);
        var request = CreateMessageRequest.builder().model("anthropic.claude-v2").build();

        Iterator<StreamEvent> stream = client.createMessageStream(request);
        assertTrue(stream.hasNext());
    }

    // --- VertexClient ---

    @Test
    void vertexClientReturnsModel() {
        var config = new ApiConfig.VertexConfig("my-project", "us-central1", "claude-sonnet-4-20250514");
        var client = new VertexClient(config);
        assertEquals("claude-sonnet-4-20250514", client.getModel());
        assertEquals("my-project", client.getProjectId());
        assertEquals("us-central1", client.getLocation());
    }

    @Test
    void vertexClientCreateMessage() {
        var config = new ApiConfig.VertexConfig("my-project", "us-central1", "claude-sonnet-4-20250514");
        var client = new VertexClient(config);
        var request = CreateMessageRequest.builder().model("claude-sonnet-4-20250514").build();

        ApiMessage response = client.createMessage(request);
        assertNotNull(response);
    }

    @Test
    void vertexClientStreamReturnsEvents() {
        var config = new ApiConfig.VertexConfig("my-project", "us-central1", "claude-sonnet-4-20250514");
        var client = new VertexClient(config);
        var request = CreateMessageRequest.builder().model("claude-sonnet-4-20250514").build();

        Iterator<StreamEvent> stream = client.createMessageStream(request);
        assertTrue(stream.hasNext());
    }

    // --- LlmClientFactory ---

    @Test
    void factoryCreatesCorrectClientType() {
        var anthropicConfig = ApiConfig.anthropic("sk-test", "claude-sonnet-4-20250514");
        assertInstanceOf(AnthropicSdkClient.class, LlmClientFactory.create(anthropicConfig));

        var openaiConfig = new ApiConfig(ApiConfig.ApiProvider.OPENAI_COMPAT,
            null, new ApiConfig.OpenAiConfig("sk-test", "gpt-4", "https://api.openai.com"), null, null);
        assertInstanceOf(OpenAiCompatClient.class, LlmClientFactory.create(openaiConfig));

        var bedrockConfig = new ApiConfig(ApiConfig.ApiProvider.BEDROCK,
            null, null, new ApiConfig.BedrockConfig("us-east-1", "claude-v2"), null);
        assertInstanceOf(BedrockClient.class, LlmClientFactory.create(bedrockConfig));

        var vertexConfig = new ApiConfig(ApiConfig.ApiProvider.VERTEX,
            null, null, null, new ApiConfig.VertexConfig("proj", "us-central1", "claude-v2"));
        assertInstanceOf(VertexClient.class, LlmClientFactory.create(vertexConfig));
    }

    // --- Prompt Caching ---

    @Test
    void cacheControlEphemeral() {
        var cc = CreateMessageRequest.CacheControl.ephemeral();
        assertEquals("ephemeral", cc.type());
    }

    @Test
    void toolDefinitionWithCacheControl() {
        JsonNode schema = JsonUtils.parseTree("{\"type\":\"object\"}");
        var tool = new CreateMessageRequest.ToolDefinition(
            "bash", "Execute command", schema, CreateMessageRequest.CacheControl.ephemeral());

        assertEquals("bash", tool.name());
        assertNotNull(tool.cacheControl());
        assertEquals("ephemeral", tool.cacheControl().type());
    }

    @Test
    void toolDefinitionWithoutCacheControl() {
        JsonNode schema = JsonUtils.parseTree("{\"type\":\"object\"}");
        var tool = new CreateMessageRequest.ToolDefinition("bash", "Execute command", schema);
        assertNull(tool.cacheControl());
    }

    @Test
    void cacheControlSerializesCorrectly() {
        JsonNode schema = JsonUtils.parseTree("{\"type\":\"object\"}");
        var tool = new CreateMessageRequest.ToolDefinition(
            "bash", "Execute command", schema, CreateMessageRequest.CacheControl.ephemeral());

        var request = CreateMessageRequest.builder()
            .model("claude-sonnet-4-20250514")
            .tools(List.of(tool))
            .build();

        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);
        JsonNode toolNode = node.get("tools").get(0);
        assertTrue(toolNode.has("cache_control"));
        assertEquals("ephemeral", toolNode.get("cache_control").get("type").asText());
    }

    // --- Thinking/Extended Thinking ---

    @Test
    void thinkingConfigEnabled() {
        var thinking = CreateMessageRequest.ThinkingConfig.enabled(10000);
        assertEquals("enabled", thinking.type());
        assertEquals(10000, thinking.budgetTokens());
    }

    @Test
    void thinkingConfigDisabled() {
        var thinking = CreateMessageRequest.ThinkingConfig.disabled();
        assertEquals("disabled", thinking.type());
        assertNull(thinking.budgetTokens());
    }

    @Test
    void requestWithThinkingConfig() {
        var request = CreateMessageRequest.builder()
            .model("claude-sonnet-4-20250514")
            .thinking(CreateMessageRequest.ThinkingConfig.enabled(5000))
            .build();

        assertNotNull(request.thinking());
        assertEquals("enabled", request.thinking().type());
        assertEquals(5000, request.thinking().budgetTokens());
    }

    @Test
    void thinkingConfigSerializesCorrectly() {
        var request = CreateMessageRequest.builder()
            .model("claude-sonnet-4-20250514")
            .thinking(CreateMessageRequest.ThinkingConfig.enabled(8000))
            .build();

        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);
        assertTrue(node.has("thinking"));
        assertEquals("enabled", node.get("thinking").get("type").asText());
        assertEquals(8000, node.get("thinking").get("budget_tokens").asInt());
    }

    @Test
    void requestWithoutThinkingOmitsField() {
        var request = CreateMessageRequest.builder()
            .model("claude-sonnet-4-20250514")
            .build();

        assertNull(request.thinking());
        String json = JsonUtils.toJson(request);
        JsonNode node = JsonUtils.parseTree(json);
        assertFalse(node.has("thinking") && !node.get("thinking").isNull());
    }

    // --- ApiMessage.stub ---

    @Test
    void apiMessageStubCreatesValidMessage() {
        ApiMessage msg = ApiMessage.stub("claude-sonnet-4-20250514", "Hello world");
        assertEquals("claude-sonnet-4-20250514", msg.model());
        assertEquals("assistant", msg.role());
        assertEquals("message", msg.type());
        assertEquals("end_turn", msg.stopReason());
        assertFalse(msg.content().isEmpty());
    }
}
