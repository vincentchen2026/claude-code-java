package com.claudecode.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Request to create a message via the Anthropic Messages API.
 * Supports prompt caching and extended thinking mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateMessageRequest(
        @JsonProperty("model") String model,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("system") String systemPrompt,
        @JsonProperty("messages") List<RequestMessage> messages,
        @JsonProperty("tools") List<ToolDefinition> tools,
        @JsonProperty("metadata") JsonNode metadata,
        @JsonProperty("stop_sequences") List<String> stopSequences,
        @JsonProperty("stream") boolean stream,
        @JsonProperty("temperature") Double temperature,
        @JsonProperty("top_p") Double topP,
        @JsonProperty("top_k") Integer topK,
        @JsonProperty("thinking") ThinkingConfig thinking
) {

    /**
     * A message in the request messages array.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RequestMessage(
            @JsonProperty("role") String role,
            @JsonProperty("content") Object content
    ) {}

    /**
     * A tool definition for the API request.
     * Supports cache_control for prompt caching.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolDefinition(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("input_schema") JsonNode inputSchema,
            @JsonProperty("cache_control") CacheControl cacheControl
    ) {
        /** Convenience constructor without cache control. */
        public ToolDefinition(String name, String description, JsonNode inputSchema) {
            this(name, description, inputSchema, null);
        }
    }

    /**
     * Cache control for prompt caching support.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CacheControl(
            @JsonProperty("type") String type
    ) {
        /** Creates an ephemeral cache control. */
        public static CacheControl ephemeral() {
            return new CacheControl("ephemeral");
        }
    }

    /**
     * Thinking/extended thinking configuration.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ThinkingConfig(
            @JsonProperty("type") String type,
            @JsonProperty("budget_tokens") Integer budgetTokens
    ) {
        /** Creates an enabled thinking config with a token budget. */
        public static ThinkingConfig enabled(int budgetTokens) {
            return new ThinkingConfig("enabled", budgetTokens);
        }

        /** Creates a disabled thinking config. */
        public static ThinkingConfig disabled() {
            return new ThinkingConfig("disabled", null);
        }
    }

    /**
     * Builder for CreateMessageRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private int maxTokens = 4096;
        private String systemPrompt;
        private List<RequestMessage> messages = List.of();
        private List<ToolDefinition> tools;
        private JsonNode metadata;
        private List<String> stopSequences;
        private boolean stream = true;
        private Double temperature;
        private Double topP;
        private Integer topK;
        private ThinkingConfig thinking;

        public Builder model(String model) { this.model = model; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder messages(List<RequestMessage> messages) { this.messages = messages; return this; }
        public Builder tools(List<ToolDefinition> tools) { this.tools = tools; return this; }
        public Builder metadata(JsonNode metadata) { this.metadata = metadata; return this; }
        public Builder stopSequences(List<String> stopSequences) { this.stopSequences = stopSequences; return this; }
        public Builder stream(boolean stream) { this.stream = stream; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder topP(Double topP) { this.topP = topP; return this; }
        public Builder topK(Integer topK) { this.topK = topK; return this; }
        public Builder thinking(ThinkingConfig thinking) { this.thinking = thinking; return this; }

        public CreateMessageRequest build() {
            return new CreateMessageRequest(
                    model, maxTokens, systemPrompt, messages, tools,
                    metadata, stopSequences, stream, temperature, topP, topK,
                    thinking
            );
        }
    }
}
