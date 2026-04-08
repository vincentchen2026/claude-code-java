package com.claudecode.services.prompt;

import java.nio.file.Path;
import java.util.List;

/**
 * Configuration for system prompt construction.
 *
 * @param basePrompt       the base system prompt template (null to use default)
 * @param toolNames        list of available tool names
 * @param mcpServerNames   list of connected MCP server names
 * @param workingDirectory the current working directory
 * @param claudeMdPaths    paths to CLAUDE.md instruction files (searched in order)
 * @param scratchpadPath   optional path to the scratchpad directory
 * @param customOverride   if non-null, replaces the entire system prompt with this value
 */
public record SystemPromptConfig(
        String basePrompt,
        List<String> toolNames,
        List<String> mcpServerNames,
        String workingDirectory,
        List<Path> claudeMdPaths,
        String scratchpadPath,
        String customOverride
) {

    /**
     * Compact constructor with defaults for null collections.
     */
    public SystemPromptConfig {
        toolNames = toolNames != null ? List.copyOf(toolNames) : List.of();
        mcpServerNames = mcpServerNames != null ? List.copyOf(mcpServerNames) : List.of();
        claudeMdPaths = claudeMdPaths != null ? List.copyOf(claudeMdPaths) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String basePrompt;
        private List<String> toolNames;
        private List<String> mcpServerNames;
        private String workingDirectory;
        private List<Path> claudeMdPaths;
        private String scratchpadPath;
        private String customOverride;

        private Builder() {}

        public Builder basePrompt(String basePrompt) {
            this.basePrompt = basePrompt;
            return this;
        }

        public Builder toolNames(List<String> toolNames) {
            this.toolNames = toolNames;
            return this;
        }

        public Builder mcpServerNames(List<String> mcpServerNames) {
            this.mcpServerNames = mcpServerNames;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder claudeMdPaths(List<Path> claudeMdPaths) {
            this.claudeMdPaths = claudeMdPaths;
            return this;
        }

        public Builder scratchpadPath(String scratchpadPath) {
            this.scratchpadPath = scratchpadPath;
            return this;
        }

        public Builder customOverride(String customOverride) {
            this.customOverride = customOverride;
            return this;
        }

        public SystemPromptConfig build() {
            return new SystemPromptConfig(
                    basePrompt, toolNames, mcpServerNames,
                    workingDirectory, claudeMdPaths, scratchpadPath,
                    customOverride
            );
        }
    }
}
