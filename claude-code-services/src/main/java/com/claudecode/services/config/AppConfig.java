package com.claudecode.services.config;

/**
 * Application configuration record. Merged from multiple sources:
 * user-level → project-level → local-level → CLI args → env vars.
 *
 * @param apiKey          Anthropic API key
 * @param model           model name (e.g. "claude-sonnet-4-20250514")
 * @param maxTokens       max output tokens per request
 * @param maxTurns        max conversation turns (tool-use loops)
 * @param maxBudgetUsd    max USD budget for the session
 * @param permissionMode  permission mode ("default", "plan", "bypass")
 * @param verbose         verbose logging
 * @param systemPrompt    custom system prompt override
 */
public record AppConfig(
    String apiKey,
    String model,
    Integer maxTokens,
    Integer maxTurns,
    Double maxBudgetUsd,
    String permissionMode,
    boolean verbose,
    String systemPrompt
) {

    /** Default configuration with sensible defaults. */
    public static final AppConfig DEFAULT = new AppConfig(
        null,
        "claude-sonnet-4-20250514",
        16384,
        null,
        null,
        "default",
        false,
        null
    );

    /**
     * Merges this config with an overlay. Non-null overlay values take precedence.
     */
    public AppConfig merge(AppConfig overlay) {
        if (overlay == null) {
            return this;
        }
        return new AppConfig(
            overlay.apiKey != null ? overlay.apiKey : this.apiKey,
            overlay.model != null ? overlay.model : this.model,
            overlay.maxTokens != null ? overlay.maxTokens : this.maxTokens,
            overlay.maxTurns != null ? overlay.maxTurns : this.maxTurns,
            overlay.maxBudgetUsd != null ? overlay.maxBudgetUsd : this.maxBudgetUsd,
            overlay.permissionMode != null ? overlay.permissionMode : this.permissionMode,
            overlay.verbose || this.verbose,
            overlay.systemPrompt != null ? overlay.systemPrompt : this.systemPrompt
        );
    }
}
