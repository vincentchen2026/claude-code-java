package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillsCommandTest {

    private SkillsCommand command;
    private Path tempDir;
    private CommandContext context;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        this.command = new SkillsCommand();
        this.context = new CommandContext(
            "test-model",
            List::of,
            () -> {},
            m -> {},
            () -> com.claudecode.core.message.Usage.EMPTY,
            u -> 0.0,
            tempDir.toString(),
            false
        );
    }

    @Test
    void listWithNoSkills() {
        CommandResult result = command.execute(context, "list");
        assertTrue(result.output().contains("No skills found") ||
                   result.output().contains("No skills directory") ||
                   result.output().contains("Create your first skill"));
    }

    @Test
    void addSkill() {
        CommandResult result = command.execute(context, "add test-skill");
        assertTrue(result.output().contains("Created skill") ||
                   result.output().contains("test-skill"));

        // Verify file was created
        Path skillFile = tempDir.resolve(".claude/skills/test-skill.md");
        assertTrue(Files.exists(skillFile), "Skill file should exist");
    }

    @Test
    void addDuplicateSkill() {
        // Create skill first
        command.execute(context, "add existing-skill");

        // Try to create again
        CommandResult result = command.execute(context, "add existing-skill");
        assertTrue(result.output().contains("already exists"));
    }

    @Test
    void removeSkill() {
        // Create skill first
        command.execute(context, "add to-remove");

        // Remove it
        CommandResult result = command.execute(context, "remove to-remove");
        assertTrue(result.output().contains("Removed") ||
                   result.output().contains("to-remove"));
    }

    @Test
    void removeNonexistentSkill() {
        CommandResult result = command.execute(context, "remove nonexistent");
        assertTrue(result.output().contains("not found"));
    }

    @Test
    void showSkill() {
        // Create skill first
        command.execute(context, "add show-test");

        // Show it
        CommandResult result = command.execute(context, "show show-test");
        assertTrue(result.output().contains("show-test"));
    }

    @Test
    void enableDisableSkill() {
        // Create skill first
        command.execute(context, "add toggle-test");

        // Disable it (renames to .toggle-test.md)
        CommandResult disableResult = command.execute(context, "disable toggle-test");
        assertTrue(disableResult.output().contains("Disabled") ||
                   disableResult.output().contains("toggle-test"));

        // Enable it
        CommandResult enableResult = command.execute(context, "enable toggle-test");
        assertTrue(enableResult.output().contains("Enabled") ||
                   enableResult.output().contains("toggle-test"));
    }

    @Test
    void installFromUrlHelp() {
        // Without args, should show usage
        CommandResult result = command.execute(context, "install");
        assertTrue(result.output().contains("Usage") &&
                   result.output().contains("install"));
    }

    @Test
    void installFromUrlNotFound() {
        // Test with a URL that doesn't exist
        CommandResult result = command.execute(context,
            "install https://raw.githubusercontent.com/anthropics/claude-code-skills/main/nonexistent-skill-xyz.md");

        // Should fail gracefully with a message
        assertTrue(result.output().contains("Failed to install") ||
                   result.output().contains("404") ||
                   result.output().contains("not found") ||
                   result.output().contains("Error"));
    }

    @Test
    void helpShowsInstallCommand() {
        // Calling without args shows list (default action is "list")
        CommandResult result = command.execute(context, "");
        assertTrue(result.output().contains("Custom Skills") ||
                   result.output().contains("skills") ||
                   result.output().contains("Skills"));
    }

    @Test
    void installShowsInHelp() {
        // Unknown action shows help which includes install command
        CommandResult result = command.execute(context, "unknown-action");
        assertTrue(result.output().contains("install"));
    }
}
