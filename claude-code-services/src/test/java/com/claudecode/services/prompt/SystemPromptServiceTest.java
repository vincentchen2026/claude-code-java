package com.claudecode.services.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SystemPromptService}.
 */
class SystemPromptServiceTest {

    private SystemPromptService service;

    @BeforeEach
    void setUp() {
        service = new SystemPromptService();
    }

    // --- System prompt assembly ---

    @Test
    void buildSystemPromptWithAllParts(@TempDir Path tempDir) throws IOException {
        // Create a CLAUDE.md file
        Path claudeMd = tempDir.resolve("CLAUDE.md");
        Files.writeString(claudeMd, "Always use tabs for indentation.");

        SystemPromptConfig config = SystemPromptConfig.builder()
                .basePrompt("You are a helpful assistant.")
                .toolNames(List.of("Bash", "Read", "Write"))
                .mcpServerNames(List.of("github-mcp", "jira-mcp"))
                .workingDirectory("/home/user/project")
                .claudeMdPaths(List.of(claudeMd))
                .scratchpadPath("/tmp/scratchpad")
                .build();

        String prompt = service.buildSystemPrompt(config);

        // Base prompt
        assertTrue(prompt.contains("You are a helpful assistant."));
        // User context
        assertTrue(prompt.contains("Available tools: Bash, Read, Write"));
        assertTrue(prompt.contains("MCP servers: github-mcp, jira-mcp"));
        assertTrue(prompt.contains("Scratchpad: /tmp/scratchpad"));
        // System context
        assertTrue(prompt.contains("CWD: /home/user/project"));
        assertTrue(prompt.contains("OS:"));
        assertTrue(prompt.contains("Date:"));
        // CLAUDE.md
        assertTrue(prompt.contains("Always use tabs for indentation."));
        assertTrue(prompt.contains("<claude_md_instructions>"));
    }

    @Test
    void buildSystemPromptUsesDefaultBaseWhenNull() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .workingDirectory("/tmp")
                .build();

        String prompt = service.buildSystemPrompt(config);

        assertTrue(prompt.contains(SystemPromptService.DEFAULT_BASE_PROMPT));
    }

    @Test
    void buildSystemPromptUsesDefaultBaseWhenBlank() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .basePrompt("   ")
                .workingDirectory("/tmp")
                .build();

        String prompt = service.buildSystemPrompt(config);

        assertTrue(prompt.contains(SystemPromptService.DEFAULT_BASE_PROMPT));
    }

    // --- Custom override ---

    @Test
    void customOverrideReplacesEntirePrompt() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .basePrompt("This should be ignored.")
                .toolNames(List.of("Bash"))
                .customOverride("My completely custom system prompt.")
                .workingDirectory("/tmp")
                .build();

        String prompt = service.buildSystemPrompt(config);

        assertEquals("My completely custom system prompt.", prompt);
        assertFalse(prompt.contains("This should be ignored."));
        assertFalse(prompt.contains("Available tools"));
    }

    @Test
    void blankCustomOverrideIsIgnored() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .basePrompt("Real base prompt.")
                .customOverride("   ")
                .workingDirectory("/tmp")
                .build();

        String prompt = service.buildSystemPrompt(config);

        assertTrue(prompt.contains("Real base prompt."));
    }

    // --- User context formatting ---

    @Test
    void userContextFormattingWithAllFields() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .toolNames(List.of("Bash", "Read"))
                .mcpServerNames(List.of("server1"))
                .scratchpadPath("/tmp/scratch")
                .workingDirectory("/tmp")
                .build();

        String userContext = service.buildUserContext(config);

        assertTrue(userContext.startsWith("<user_context>"));
        assertTrue(userContext.endsWith("</user_context>"));
        assertTrue(userContext.contains("Available tools: Bash, Read"));
        assertTrue(userContext.contains("MCP servers: server1"));
        assertTrue(userContext.contains("Scratchpad: /tmp/scratch"));
    }

    @Test
    void userContextEmptyWhenNoFields() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .workingDirectory("/tmp")
                .build();

        String userContext = service.buildUserContext(config);

        assertEquals("", userContext);
    }

    @Test
    void userContextWithOnlyTools() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .toolNames(List.of("Grep", "Glob"))
                .workingDirectory("/tmp")
                .build();

        String userContext = service.buildUserContext(config);

        assertTrue(userContext.contains("Available tools: Grep, Glob"));
        assertFalse(userContext.contains("MCP servers"));
        assertFalse(userContext.contains("Scratchpad"));
    }

    // --- System context ---

    @Test
    void systemContextContainsExpectedFields() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .workingDirectory("/my/project")
                .build();

        String systemContext = service.buildSystemContext(config);

        assertTrue(systemContext.contains("<system_context>"));
        assertTrue(systemContext.contains("OS:"));
        assertTrue(systemContext.contains("CWD: /my/project"));
        assertTrue(systemContext.contains("Date:"));
        assertTrue(systemContext.contains("</system_context>"));
    }

    @Test
    void systemContextHandlesNullWorkingDirectory() {
        SystemPromptConfig config = SystemPromptConfig.builder().build();

        String systemContext = service.buildSystemContext(config);

        assertTrue(systemContext.contains("CWD: unknown"));
    }

    // --- No CLAUDE.md ---

    @Test
    void buildSystemPromptWithNoClaudeMd() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .basePrompt("Base prompt.")
                .workingDirectory("/tmp")
                .build();

        String prompt = service.buildSystemPrompt(config);

        assertTrue(prompt.contains("Base prompt."));
        assertFalse(prompt.contains("<claude_md_instructions>"));
    }

    @Test
    void buildSystemPromptWithNonExistentClaudeMdPaths() {
        SystemPromptConfig config = SystemPromptConfig.builder()
                .basePrompt("Base prompt.")
                .claudeMdPaths(List.of(
                        Path.of("/nonexistent/path/CLAUDE.md"),
                        Path.of("/also/missing/CLAUDE.md")
                ))
                .workingDirectory("/tmp")
                .build();

        String prompt = service.buildSystemPrompt(config);

        assertTrue(prompt.contains("Base prompt."));
        assertFalse(prompt.contains("<claude_md_instructions>"));
    }
}
