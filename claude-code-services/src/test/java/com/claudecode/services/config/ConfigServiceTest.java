package com.claudecode.services.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService createService(Map<String, String> env) {
        return new ConfigService(tempDir, env::get);
    }

    @Test
    void loadConfigReturnsDefaultsWhenNoSources() {
        ConfigService service = createService(Map.of());
        AppConfig config = service.loadConfig(null);

        assertEquals("claude-sonnet-4-20250514", config.model());
        assertEquals(16384, config.maxTokens());
        assertEquals("default", config.permissionMode());
    }

    @Test
    void envVarsOverrideDefaults() {
        Map<String, String> env = new HashMap<>();
        env.put("ANTHROPIC_API_KEY", "sk-env-key");
        env.put("CLAUDE_MODEL", "opus");
        env.put("CLAUDE_MAX_TOKENS", "8192");

        ConfigService service = createService(env);
        AppConfig config = service.loadConfig(null);

        assertEquals("sk-env-key", config.apiKey());
        assertEquals("opus", config.model());
        assertEquals(8192, config.maxTokens());
    }

    @Test
    void cliOverridesOverrideEverything() {
        Map<String, String> env = new HashMap<>();
        env.put("CLAUDE_MODEL", "opus");

        AppConfig cliOverrides = new AppConfig(
            "sk-cli", "haiku", null, null, null, null, false, null);

        ConfigService service = createService(env);
        AppConfig config = service.loadConfig(cliOverrides);

        assertEquals("sk-cli", config.apiKey());
        assertEquals("haiku", config.model()); // CLI wins over env
    }

    @Test
    void projectSettingsOverrideUserSettings() throws IOException {
        // Create project-level settings
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("settings.json"), """
            {"model": "project-model", "maxTokens": 2048}""");

        ConfigService service = createService(Map.of());
        AppConfig config = service.loadConfig(null);

        assertEquals("project-model", config.model());
        assertEquals(2048, config.maxTokens());
    }

    @Test
    void localSettingsOverrideProjectSettings() throws IOException {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("settings.json"), """
            {"model": "project-model"}""");
        Files.writeString(claudeDir.resolve("settings.local.json"), """
            {"model": "local-model"}""");

        ConfigService service = createService(Map.of());
        AppConfig config = service.loadConfig(null);

        assertEquals("local-model", config.model());
    }

    @Test
    void jsoncCommentsInSettingsFiles() throws IOException {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("settings.json"), """
            {
              // Use opus for this project
              "model": "opus",
              "maxTokens": 4096,
            }""");

        ConfigService service = createService(Map.of());
        AppConfig config = service.loadConfig(null);

        assertEquals("opus", config.model());
        assertEquals(4096, config.maxTokens());
    }

    @Test
    void invalidEnvVarsAreIgnored() {
        Map<String, String> env = new HashMap<>();
        env.put("CLAUDE_MAX_TOKENS", "not-a-number");

        ConfigService service = createService(env);
        AppConfig config = service.loadConfig(null);

        assertEquals(16384, config.maxTokens()); // default preserved
    }

    @Test
    void maxBudgetFromEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("CLAUDE_MAX_BUDGET_USD", "5.50");

        ConfigService service = createService(env);
        AppConfig config = service.loadConfig(null);

        assertEquals(5.50, config.maxBudgetUsd(), 0.001);
    }

    @Test
    void maxTurnsFromEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("CLAUDE_MAX_TURNS", "25");

        ConfigService service = createService(env);
        AppConfig config = service.loadConfig(null);

        assertEquals(25, config.maxTurns());
    }

    @Test
    void fullMergeOrder() throws IOException {
        // Project settings set model
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("settings.json"), """
            {"model": "project-model", "maxTokens": 2048}""");

        // Local settings override model
        Files.writeString(claudeDir.resolve("settings.local.json"), """
            {"model": "local-model"}""");

        // Env overrides maxTokens
        Map<String, String> env = new HashMap<>();
        env.put("CLAUDE_MAX_TOKENS", "4096");

        // CLI overrides model
        AppConfig cli = new AppConfig(null, "cli-model", null, null, null, null, false, null);

        ConfigService service = createService(env);
        AppConfig config = service.loadConfig(cli);

        assertEquals("cli-model", config.model());   // CLI wins
        assertEquals(4096, config.maxTokens());       // env wins over project
    }
}
