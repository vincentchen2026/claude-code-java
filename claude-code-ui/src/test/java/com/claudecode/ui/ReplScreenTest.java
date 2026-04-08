package com.claudecode.ui;

import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandRegistry;
import com.claudecode.commands.CommandResult;
import com.claudecode.commands.Command;
import com.claudecode.core.engine.QueryEngine;
import com.claudecode.core.engine.QueryEngineConfig;
import com.claudecode.core.engine.SubmitOptions;
import com.claudecode.core.message.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReplScreen: slash command detection, dispatch, and message rendering.
 */
class ReplScreenTest {

    @Test
    void isSlashCommand_detectsSlashPrefix() {
        assertTrue(ReplScreen.isSlashCommand("/help"));
        assertTrue(ReplScreen.isSlashCommand("/exit"));
        assertTrue(ReplScreen.isSlashCommand("/model sonnet"));
        assertFalse(ReplScreen.isSlashCommand("hello"));
        assertFalse(ReplScreen.isSlashCommand(""));
        assertFalse(ReplScreen.isSlashCommand(null));
    }

    @Test
    void isContentMessage_identifiesContentTypes() {
        // Assistant is content
        SDKMessage.Assistant assistant = new SDKMessage.Assistant(
                new AssistantMessage("id", AssistantContent.of(List.of(new TextBlock("hi")))),
                Usage.EMPTY);
        assertTrue(ReplScreen.isContentMessage(assistant));

        // StreamEvent is content
        SDKMessage.StreamEvent stream = new SDKMessage.StreamEvent("content_block_delta", "text");
        assertTrue(ReplScreen.isContentMessage(stream));

        // Error is content
        SDKMessage.Error error = new SDKMessage.Error(new RuntimeException("fail"));
        assertTrue(ReplScreen.isContentMessage(error));

        // Result is content
        SDKMessage.Result result = new SDKMessage.Result("success", List.of(), Usage.EMPTY, "sess1");
        assertTrue(ReplScreen.isContentMessage(result));

        // System is NOT content (spinner should keep going)
        SDKMessage.System system = new SDKMessage.System(
                new SystemMessage("id", "info", "info", "loading"));
        assertFalse(ReplScreen.isContentMessage(system));

        // Progress is NOT content
        SDKMessage.Progress progress = new SDKMessage.Progress(
                new ProgressMessage("id", "working"));
        assertFalse(ReplScreen.isContentMessage(progress));
    }

    @Test
    void handleSlashCommand_dispatchesToRegistry() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new Command() {
            @Override public String name() { return "test"; }
            @Override public String description() { return "test command"; }
            @Override public CommandResult execute(CommandContext ctx, String args) {
                return CommandResult.of("test output: " + args);
            }
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true);
        TerminalRenderer terminal = createDumbTerminal();
        InputReader inputReader = null; // Not needed for this test path

        // We test the dispatch logic via CommandRegistry directly
        CommandResult result = registry.dispatch("/test hello", CommandContext.minimal());
        assertEquals("test output: hello", result.output());
        assertFalse(result.shouldExit());
    }

    @Test
    void handleSlashCommand_exitCommandSetsExitFlag() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new Command() {
            @Override public String name() { return "exit"; }
            @Override public String description() { return "exit"; }
            @Override public List<String> aliases() { return List.of("quit"); }
            @Override public CommandResult execute(CommandContext ctx, String args) {
                return CommandResult.exit("Goodbye!");
            }
        });

        CommandResult result = registry.dispatch("/exit", CommandContext.minimal());
        assertTrue(result.shouldExit());
        assertEquals("Goodbye!", result.output());
    }

    @Test
    void handleSlashCommand_unknownCommandReturnsMessage() {
        CommandRegistry registry = new CommandRegistry();
        CommandResult result = registry.dispatch("/unknown", CommandContext.minimal());
        assertTrue(result.output().contains("Unknown command"));
    }

    private TerminalRenderer createDumbTerminal() {
        return TerminalRenderer.createDumb();
    }
}
