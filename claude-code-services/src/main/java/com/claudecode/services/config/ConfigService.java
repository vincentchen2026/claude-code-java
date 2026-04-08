package com.claudecode.services.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Multi-level configuration loader.
 * <p>
 * Load order (later values override earlier):
 * <ol>
 *   <li>Defaults</li>
 *   <li>User-level: ~/.claude/settings.json</li>
 *   <li>Project-level: .claude/settings.json</li>
 *   <li>Local-level: .claude/settings.local.json</li>
 *   <li>Environment variables</li>
 *   <li>CLI arguments</li>
 * </ol>
 */
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final Path projectDir;
    private final Function<String, String> envLookup;

    public ConfigService(Path projectDir) {
        this(projectDir, System::getenv);
    }

    /** Constructor with injectable env lookup (for testing). */
    public ConfigService(Path projectDir, Function<String, String> envLookup) {
        this.projectDir = projectDir;
        this.envLookup = envLookup;
    }

    /**
     * Loads and merges configuration from all levels, then applies CLI overrides.
     *
     * @param cliOverrides CLI argument overrides (may be null)
     * @return fully merged AppConfig
     */
    public AppConfig loadConfig(AppConfig cliOverrides) {
        AppConfig config = AppConfig.DEFAULT;

        // 1. User-level settings
        config = config.merge(loadSettingsFile(getUserSettingsPath()));

        // 2. Project-level settings
        if (projectDir != null) {
            config = config.merge(loadSettingsFile(projectDir.resolve(".claude/settings.json")));
        }

        // 3. Local-level settings
        if (projectDir != null) {
            config = config.merge(loadSettingsFile(projectDir.resolve(".claude/settings.local.json")));
        }

        // 4. Environment variables
        config = config.merge(loadFromEnvironment());

        // 5. CLI arguments
        config = config.merge(cliOverrides);

        return config;
    }

    /**
     * Reads environment variables and maps them to an AppConfig overlay.
     */
    AppConfig loadFromEnvironment() {
        String apiKey = envLookup.apply("ANTHROPIC_API_KEY");
        String model = envLookup.apply("CLAUDE_MODEL");
        Integer maxTokens = parseIntEnv("CLAUDE_MAX_TOKENS");
        Integer maxTurns = parseIntEnv("CLAUDE_MAX_TURNS");
        Double maxBudget = parseDoubleEnv("CLAUDE_MAX_BUDGET_USD");

        // Only create overlay if at least one env var is set
        if (apiKey == null && model == null && maxTokens == null
                && maxTurns == null && maxBudget == null) {
            return null;
        }

        return new AppConfig(apiKey, model, maxTokens, maxTurns, maxBudget,
                null, false, null);
    }

    /**
     * Loads a settings.json file and converts it to an AppConfig overlay.
     */
    AppConfig loadSettingsFile(Path path) {
        JsonNode node = SettingsManager.readSettings(path);
        if (node == null) {
            return null;
        }
        return jsonNodeToConfig(node);
    }

    /**
     * Converts a JsonNode to an AppConfig overlay (null fields = not set).
     */
    static AppConfig jsonNodeToConfig(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        return new AppConfig(
            textOrNull(node, "apiKey"),
            textOrNull(node, "model"),
            intOrNull(node, "maxTokens"),
            intOrNull(node, "maxTurns"),
            doubleOrNull(node, "maxBudgetUsd"),
            textOrNull(node, "permissionMode"),
            node.has("verbose") && node.get("verbose").asBoolean(false),
            textOrNull(node, "systemPrompt")
        );
    }

    Path getUserSettingsPath() {
        return Path.of(System.getProperty("user.home"), ".claude", "settings.json");
    }

    private Integer parseIntEnv(String name) {
        String val = envLookup.apply(name);
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val.strip());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for {}: {}", name, val);
            return null;
        }
    }

    private Double parseDoubleEnv(String name) {
        String val = envLookup.apply(name);
        if (val == null || val.isBlank()) return null;
        try {
            return Double.parseDouble(val.strip());
        } catch (NumberFormatException e) {
            log.warn("Invalid double for {}: {}", name, val);
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && child.isTextual()) ? child.asText() : null;
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && child.isNumber()) ? child.asInt() : null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && child.isNumber()) ? child.asDouble() : null;
    }
}
