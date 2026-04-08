package com.claudecode.api;

import java.util.Optional;

/**
 * Configuration for API client creation.
 */
public record ApiConfig(
        ApiProvider provider,
        AnthropicConfig anthropic,
        OpenAiConfig openai,
        BedrockConfig bedrock,
        VertexConfig vertex
) {

    public enum ApiProvider {
        ANTHROPIC,
        OPENAI_COMPAT,
        BEDROCK,
        VERTEX
    }

    /**
     * Creates a config for the Anthropic provider.
     */
    public static ApiConfig anthropic(String apiKey, String model) {
        return new ApiConfig(
                ApiProvider.ANTHROPIC,
                new AnthropicConfig(apiKey, model, Optional.empty()),
                null, null, null
        );
    }

    public record AnthropicConfig(
            String apiKey,
            String model,
            Optional<String> baseUrl
    ) {}

    public record OpenAiConfig(
            String apiKey,
            String model,
            String baseUrl
    ) {}

    public record BedrockConfig(
            String region,
            String model
    ) {}

    public record VertexConfig(
            String projectId,
            String location,
            String model
    ) {}
}
