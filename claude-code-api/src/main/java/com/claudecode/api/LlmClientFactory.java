package com.claudecode.api;

/**
 * Factory for creating LLM clients based on provider configuration.
 * Currently only AnthropicSdkClient has a full implementation.
 * Other providers are stubs that throw UnsupportedOperationException.
 */
public final class LlmClientFactory {

    private LlmClientFactory() {}

    /**
     * Creates an LlmClient for the given configuration.
     */
    public static LlmClient create(ApiConfig config) {
        return switch (config.provider()) {
            case ANTHROPIC -> new AnthropicSdkClient(config.anthropic());
            case OPENAI_COMPAT -> new OpenAiCompatClient(config.openai());
            case BEDROCK -> new BedrockClient(config.bedrock());
            case VERTEX -> new VertexClient(config.vertex());
        };
    }

    /**
     * Creates an LlmClient for the given configuration with a custom HTTP executor.
     * Used for testing to inject mock HTTP responses.
     */
    public static LlmClient create(ApiConfig config, HttpExecutor httpExecutor) {
        if (config.provider() != ApiConfig.ApiProvider.OPENAI_COMPAT) {
            throw new IllegalArgumentException("Custom HTTP executor only supported for OPENAI_COMPAT");
        }
        return new OpenAiCompatClient(config.openai(), httpExecutor);
    }
}
