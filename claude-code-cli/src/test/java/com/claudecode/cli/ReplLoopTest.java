package com.claudecode.cli;

import com.claudecode.core.engine.QueryEngine;
import com.claudecode.core.engine.QueryEngineConfig;
import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.StreamingClient.StreamingEvent;
import com.claudecode.core.message.Usage;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the REPL loop.
 */
class ReplLoopTest {

    @Test
    void testReplExitCommand() {
        StreamingClient mockClient = new ClaudeCodeCliTest.MockStreamingClient("response");
        QueryEngineConfig config = QueryEngineConfig.builder()
            .llmClient(mockClient)
            .model("test-model")
            .systemPrompt("test")
            .maxTokens(100)
            .build();
        QueryEngine engine = new QueryEngine(config);

        // Simulate user typing "/exit"
        String input = "/exit\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);

        ReplLoop repl = new ReplLoop(engine, out, reader);
        int exitCode = repl.run();

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("Goodbye!"),
            "Should print goodbye message, got: " + sw.toString());
    }

    @Test
    void testReplEof() {
        StreamingClient mockClient = new ClaudeCodeCliTest.MockStreamingClient("response");
        QueryEngineConfig config = QueryEngineConfig.builder()
            .llmClient(mockClient)
            .model("test-model")
            .systemPrompt("test")
            .maxTokens(100)
            .build();
        QueryEngine engine = new QueryEngine(config);

        // Empty input simulates EOF (Ctrl+D)
        String input = "";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);

        ReplLoop repl = new ReplLoop(engine, out, reader);
        int exitCode = repl.run();

        assertEquals(0, exitCode);
    }

    @Test
    void testReplProcessesPromptAndExits() {
        StreamingClient mockClient = new ClaudeCodeCliTest.MockStreamingClient("Hello from Claude!");
        QueryEngineConfig config = QueryEngineConfig.builder()
            .llmClient(mockClient)
            .model("test-model")
            .systemPrompt("test")
            .maxTokens(100)
            .build();
        QueryEngine engine = new QueryEngine(config);

        // User sends a message then exits
        String input = "Hello\n/exit\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);

        ReplLoop repl = new ReplLoop(engine, out, reader);
        int exitCode = repl.run();

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("Hello from Claude!"),
            "Should contain mock response, got: " + output);
        assertTrue(output.contains("Goodbye!"),
            "Should print goodbye, got: " + output);
    }

    @Test
    void testReplSkipsEmptyLines() {
        StreamingClient mockClient = new ClaudeCodeCliTest.MockStreamingClient("response");
        QueryEngineConfig config = QueryEngineConfig.builder()
            .llmClient(mockClient)
            .model("test-model")
            .systemPrompt("test")
            .maxTokens(100)
            .build();
        QueryEngine engine = new QueryEngine(config);

        // Empty lines followed by exit
        String input = "\n\n  \n/exit\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw, true);

        ReplLoop repl = new ReplLoop(engine, out, reader);
        int exitCode = repl.run();

        assertEquals(0, exitCode);
        // Should not have processed any prompts (no "response" in output)
        String output = sw.toString();
        assertFalse(output.contains("response"),
            "Should not process empty lines as prompts");
    }
}
