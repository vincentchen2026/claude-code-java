package com.claudecode.cli;

import com.claudecode.api.AnthropicSdkClient;
import com.claudecode.api.ApiConfig;
import com.claudecode.api.LlmClient;
import com.claudecode.core.engine.StreamingClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads configuration for the CLI from environment variables and config files.
 */
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String ENV_API_KEY = "ANTHROPIC_API_KEY";
    private static final String ENV_BASE_URL = "ANTHROPIC_BASE_URL";
    private static final String CONFIG_DIR = ".claude";
    private static final String CONFIG_FILE = "config.json";

    /**
     * Resolves the API key from (in priority order):
     * 1. CLI argument (if provided)
     * 2. ANTHROPIC_API_KEY environment variable
     * 3. ~/.claude/config.json file
     *
     * @param cliApiKey API key from CLI argument (may be null)
     * @return resolved API key
     * @throws ConfigException if no API key can be found
     */
    public String resolveApiKey(String cliApiKey) {
        // 1. CLI argument
        if (cliApiKey != null && !cliApiKey.isBlank()) {
            log.debug("Using API key from CLI argument");
            return cliApiKey;
        }

        // 2. Environment variable
        String envKey = getEnvironmentVariable(ENV_API_KEY);
        if (envKey != null && !envKey.isBlank()) {
            log.debug("Using API key from {} environment variable", ENV_API_KEY);
            return envKey;
        }

        // 3. Config file
        Optional<String> fileKey = loadApiKeyFromConfigFile();
        if (fileKey.isPresent()) {
            log.debug("Using API key from config file");
            return fileKey.get();
        }

        throw new ConfigException(
            "No API key found. Set " + ENV_API_KEY + " environment variable, " +
            "pass --api-key, or add it to ~/" + CONFIG_DIR + "/" + CONFIG_FILE
        );
    }

    /**
     * Resolves the API base URL from ANTHROPIC_BASE_URL environment variable.
     * Returns null if not set (will use default Anthropic URL).
     */
    public String resolveBaseUrl() {
        String envUrl = getEnvironmentVariable(ENV_BASE_URL);
        if (envUrl != null && !envUrl.isBlank()) {
            log.debug("Using base URL from {} environment variable: {}", ENV_BASE_URL, envUrl);
            return envUrl;
        }
        return null;
    }

    /**
     * Creates a StreamingClient from the resolved API key, model, and optional base URL.
     */
    public StreamingClient createStreamingClient(String apiKey, String model, String baseUrl) {
        Optional<String> optBaseUrl = baseUrl != null ? Optional.of(baseUrl) : Optional.empty();
        ApiConfig config = new ApiConfig(
            ApiConfig.ApiProvider.ANTHROPIC,
            new ApiConfig.AnthropicConfig(apiKey, model, optBaseUrl),
            null, null, null
        );
        LlmClient llmClient = new AnthropicSdkClient(config.anthropic());
        return new LlmClientAdapter(llmClient);
    }

    /**
     * Creates a StreamingClient from the resolved API key and model (default base URL).
     */
    public StreamingClient createStreamingClient(String apiKey, String model) {
        return createStreamingClient(apiKey, model, null);
    }

    /**
     * Reads the API key from ~/.claude/config.json.
     */
    Optional<String> loadApiKeyFromConfigFile() {
        Path configPath = getConfigFilePath();
        if (!Files.exists(configPath)) {
            return Optional.empty();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(configPath.toFile());
            JsonNode apiKeyNode = root.get("apiKey");
            if (apiKeyNode != null && apiKeyNode.isTextual()) {
                String key = apiKeyNode.asText();
                if (!key.isBlank()) {
                    return Optional.of(key);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read config file: {}", configPath, e);
        }

        return Optional.empty();
    }

    // Visible for testing
    String getEnvironmentVariable(String name) {
        return System.getenv(name);
    }

    // Visible for testing
    Path getConfigFilePath() {
        return Path.of(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE);
    }

    /**
     * Thrown when configuration is invalid or missing.
     */
    public static class ConfigException extends RuntimeException {
        public ConfigException(String message) {
            super(message);
        }
    }
}
