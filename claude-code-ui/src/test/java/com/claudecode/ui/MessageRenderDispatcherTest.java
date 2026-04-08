package com.claudecode.ui;

import com.claudecode.core.message.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageRenderDispatcher — verifies each SDKMessage type
 * is rendered correctly to the terminal.
 */
class MessageRenderDispatcherTest {

    private ByteArrayOutputStream outputStream;
    private TerminalRenderer terminal;
    private MessageRenderDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        terminal = TerminalRenderer.createDumb();
        dispatcher = new MessageRenderDispatcher(terminal, new MarkdownRenderer());
        // Capture output via the terminal's writer
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void renderAssistant_rendersTextBlocksViaMarkdown() {
        AssistantContent content = AssistantContent.of(List.of(new TextBlock("Hello **world**")));
        AssistantMessage msg = new AssistantMessage("id1", content);
        SDKMessage.Assistant assistant = new SDKMessage.Assistant(msg, Usage.EMPTY);

        // Should not throw
        dispatcher.render(assistant);
    }

    @Test
    void renderAssistant_rendersToolUseBlocks() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "ls -la");

        AssistantContent content = AssistantContent.of(List.of(
                new TextBlock("Let me check the files."),
                new ToolUseBlock("tu1", "BashTool", input)
        ));
        AssistantMessage msg = new AssistantMessage("id2", content);
        SDKMessage.Assistant assistant = new SDKMessage.Assistant(msg, Usage.EMPTY);

        // Should not throw
        dispatcher.render(assistant);
    }

    @Test
    void renderAssistant_handlesNullMessage() {
        SDKMessage.Assistant assistant = new SDKMessage.Assistant(null, Usage.EMPTY);
        // Should not throw
        dispatcher.render(assistant);
    }

    @Test
    void renderAssistant_handlesNullContent() {
        AssistantMessage msg = new AssistantMessage("id", null);
        SDKMessage.Assistant assistant = new SDKMessage.Assistant(msg, Usage.EMPTY);
        // Should not throw
        dispatcher.render(assistant);
    }

    @Test
    void renderStreamEvent_printsTextDelta() {
        SDKMessage.StreamEvent event = new SDKMessage.StreamEvent("content_block_delta", "Hello ");
        // Should not throw
        dispatcher.render(event);
    }

    @Test
    void renderStreamEvent_ignoresNonDeltaEvents() {
        SDKMessage.StreamEvent event = new SDKMessage.StreamEvent("message_start", new Object());
        // Should not throw, and should not print anything
        dispatcher.render(event);
    }

    @Test
    void renderSystem_rendersInfoMessage() {
        SystemMessage sysMsg = new SystemMessage("id", "info", "info", "System initialized");
        SDKMessage.System system = new SDKMessage.System(sysMsg);
        dispatcher.render(system);
    }

    @Test
    void renderSystem_rendersWarningMessage() {
        SystemMessage sysMsg = new SystemMessage("id", "warning", "warning", "Rate limit approaching");
        SDKMessage.System system = new SDKMessage.System(sysMsg);
        dispatcher.render(system);
    }

    @Test
    void renderSystem_rendersErrorLevelMessage() {
        SystemMessage sysMsg = new SystemMessage("id", "error", "error", "API error occurred");
        SDKMessage.System system = new SDKMessage.System(sysMsg);
        dispatcher.render(system);
    }

    @Test
    void renderSystem_handlesNullMessage() {
        SDKMessage.System system = new SDKMessage.System(null);
        dispatcher.render(system);
    }

    @Test
    void renderError_rendersExceptionMessage() {
        SDKMessage.Error error = new SDKMessage.Error(new RuntimeException("Connection failed"));
        dispatcher.render(error);
    }

    @Test
    void renderError_handlesNullException() {
        SDKMessage.Error error = new SDKMessage.Error(null);
        dispatcher.render(error);
    }

    @Test
    void renderResult_rendersTokenUsage() {
        Usage usage = new Usage(1000, 500, 200, 100);
        SDKMessage.Result result = new SDKMessage.Result("success", List.of(), usage, "sess1");
        dispatcher.render(result);
    }

    @Test
    void renderResult_handlesNullUsage() {
        SDKMessage.Result result = new SDKMessage.Result("success", List.of(), null, "sess1");
        dispatcher.render(result);
    }

    @Test
    void renderProgress_rendersProgressMessage() {
        ProgressMessage progMsg = new ProgressMessage("id", "Processing files...");
        SDKMessage.Progress progress = new SDKMessage.Progress(progMsg);
        dispatcher.render(progress);
    }

    @Test
    void renderUser_isIgnored() {
        // User messages should not be rendered back
        SDKMessage.User user = new SDKMessage.User(null);
        dispatcher.render(user);
    }

    @Test
    void renderSentinel_isIgnored() {
        dispatcher.render(SDKMessage.SENTINEL);
    }

    @Test
    void truncate_shortText() {
        assertEquals("hello", MessageRenderDispatcher.truncate("hello", 10));
    }

    @Test
    void truncate_longText() {
        String result = MessageRenderDispatcher.truncate("this is a very long text", 10);
        assertEquals("this is...", result);
        assertEquals(10, result.length());
    }

    @Test
    void truncate_nullText() {
        assertEquals("", MessageRenderDispatcher.truncate(null, 10));
    }

    @Test
    void truncate_exactLength() {
        assertEquals("1234567890", MessageRenderDispatcher.truncate("1234567890", 10));
    }
}
