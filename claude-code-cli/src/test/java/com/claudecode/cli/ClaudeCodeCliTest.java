package com.claudecode.cli;

import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.StreamingClient.StreamingEvent;
import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.Usage;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end smoke tests for the CLI entry point.
 */
class ClaudeCodeCliTest {

    @Test
    void testVersionFlag() {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(new ClaudeCodeCli());
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute("--version");
        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("claude-code-java 0.1.0"),
            "Version output should contain version string, got: " + sw.toString());
    }

    @Test
    void testHelpFlag() {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(new ClaudeCodeCli());
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("claude"), "Help should mention command name");
        assertTrue(output.contains("--model"), "Help should list --model option");
        assertTrue(output.contains("--api-key"), "Help should list --api-key option");
        assertTrue(output.contains("--max-tokens"), "Help should list --max-tokens option");
        assertTrue(output.contains("--output-format"), "Help should list --output-format option");
        assertTrue(output.contains("--no-interactive"), "Help should list --no-interactive option");
    }

    @Test
    void testCliCanBeInstantiatedWithPicocli() {
        ClaudeCodeCli cli = new ClaudeCodeCli();
        CommandLine cmd = new CommandLine(cli);
        assertNotNull(cmd);
        assertEquals("claude", cmd.getCommandName());
    }

    @Test
    void testOptionParsing() {
        ClaudeCodeCli cli = new ClaudeCodeCli();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs(
            "--model", "claude-opus-4-20250514",
            "--api-key", "sk-test-key",
            "--system-prompt", "Be helpful",
            "--max-tokens", "8192",
            "--max-turns", "50",
            "--max-budget-usd", "5.0",
            "--output-format", "json",
            "--no-interactive",
            "Hello Claude"
        );

        assertEquals("claude-opus-4-20250514", cli.getModel());
        assertEquals("sk-test-key", cli.getApiKey());
        assertEquals("Be helpful", cli.getSystemPrompt());
        assertEquals(8192, cli.getMaxTokens());
        assertEquals(50, cli.getMaxTurns());
        assertEquals(5.0, cli.getMaxBudgetUsd(), 0.001);
        assertEquals("json", cli.getOutputFormat());
        assertTrue(cli.isNoInteractive());
        assertEquals("Hello Claude", cli.getInitialPrompt());
    }

    @Test
    void testNonInteractiveModeWithMockClient() {
        // Create a mock streaming client that returns a simple response
        StreamingClient mockClient = new MockStreamingClient("Hello! I'm Claude.");

        ClaudeCodeCli cli = new ClaudeCodeCli();
        cli.setStreamingClientOverride(mockClient);

        StringWriter sw = new StringWriter();
        cli.setOutputWriter(new PrintWriter(sw, true));

        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute("--no-interactive", "Hi there");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("Hello! I'm Claude."),
            "Output should contain mock response, got: " + sw.toString());
    }

    @Test
    void testDefaultValues() {
        ClaudeCodeCli cli = new ClaudeCodeCli();
        new CommandLine(cli).parseArgs();

        assertEquals(16384, cli.getMaxTokens());
        assertEquals(100, cli.getMaxTurns());
        assertEquals(-1.0, cli.getMaxBudgetUsd(), 0.001);
        assertEquals("text", cli.getOutputFormat());
        assertFalse(cli.isNoInteractive());
        assertNull(cli.getInitialPrompt());
        assertNull(cli.getModel());
        assertNull(cli.getApiKey());
    }

    /**
     * A mock StreamingClient that returns a fixed text response.
     */
    static class MockStreamingClient implements StreamingClient {

        private final String responseText;

        MockStreamingClient(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public Iterator<StreamingEvent> createStream(StreamRequest request) {
            return new Iterator<>() {
                private int step = 0;

                @Override
                public boolean hasNext() {
                    return step < 3;
                }

                @Override
                public StreamingEvent next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return switch (step++) {
                        case 0 -> new StreamingEvent.MessageStartEvent(
                            "msg_001", "claude-sonnet-4-20250514",
                            List.of(), new Usage(10, 0, 0, 0)
                        );
                        case 1 -> new StreamingEvent.ContentBlockDeltaEvent(
                            0, "text_delta", responseText
                        );
                        case 2 -> new StreamingEvent.MessageDeltaEvent(
                            "end_turn", new Usage(0, 20, 0, 0)
                        );
                        default -> throw new NoSuchElementException();
                    };
                }
            };
        }

        @Override
        public String getModel() {
            return "claude-sonnet-4-20250514";
        }
    }
}
