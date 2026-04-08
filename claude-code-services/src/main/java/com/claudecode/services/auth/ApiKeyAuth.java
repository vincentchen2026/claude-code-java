package com.claudecode.services.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * API Key authentication — reads from environment variable or config file.
 */
public class ApiKeyAuth {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuth.class);
    private static final String ENV_VAR = "ANTHROPIC_API_KEY";
    private static final String CONFIG_FILE = ".claude/api_key";

    /**
     * Resolves the API key from environment variable, then config file.
     */
    public static Optional<String> resolve() {
        // 1. Environment variable (highest priority)
        String envKey = System.getenv(ENV_VAR);
        if (envKey != null && !envKey.isBlank()) {
            log.debug("API key resolved from environment variable");
            return Optional.of(envKey.trim());
        }

        // 2. Config file in home directory
        Path configPath = Path.of(System.getProperty("user.home")).resolve(CONFIG_FILE);
        return readFromFile(configPath);
    }

    /**
     * Resolves API key with an explicit config path.
     */
    public static Optional<String> resolve(Path configPath) {
        String envKey = System.getenv(ENV_VAR);
        if (envKey != null && !envKey.isBlank()) {
            return Optional.of(envKey.trim());
        }
        return readFromFile(configPath);
    }

    private static Optional<String> readFromFile(Path path) {
        try {
            if (Files.exists(path)) {
                String key = Files.readString(path).trim();
                if (!key.isBlank()) {
                    log.debug("API key resolved from config file: {}", path);
                    return Optional.of(key);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read API key from {}: {}", path, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Validates that an API key has the expected format.
     */
    public static boolean isValidFormat(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() > 10;
    }
}
