package com.claudecode.core.engine;

import com.claudecode.core.message.Message;
import com.claudecode.core.message.MessageContent;
import com.claudecode.core.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryEngineConfigTest {

    private static final StreamingClient NOOP_CLIENT = new StreamingClient() {
        @Override
        public Iterator<StreamingEvent> createStream(StreamRequest request) {
            return Collections.emptyIterator();
        }
        @Override
        public String getModel() { return "test-model"; }
    };

    @Test
    void builderRequiresLlmClient() {
        assertThrows(IllegalStateException.class, () ->
            QueryEngineConfig.builder().build()
        );
    }

    @Test
    void builderWithDefaults() {
        var config = QueryEngineConfig.builder()
            .llmClient(NOOP_CLIENT)
            .build();

        assertSame(NOOP_CLIENT, config.llmClient());
        assertEquals("claude-sonnet-4-20250514", config.model());
        assertEquals("", config.systemPrompt());
        assertEquals(16384, config.maxTokens());
        assertEquals(100, config.maxTurns());
        assertEquals(-1.0, config.maxBudgetUsd());
        assertTrue(config.initialMessages().isEmpty());
        assertNull(config.abortController());
        assertTrue(config.tools().isEmpty());
        assertTrue(config.readFileCache().isEmpty());
    }

    @Test
    void builderWithAllFields() {
        var abort = new AbortController();
        var messages = List.<Message>of(
            new UserMessage("u1", MessageContent.ofText("hello"))
        );
        var tools = List.of("BashTool", "FileReadTool");
        var cache = Map.of("file.txt", "content");

        var config = QueryEngineConfig.builder()
            .llmClient(NOOP_CLIENT)
            .model("claude-opus-4-20250514")
            .systemPrompt("You are helpful.")
            .maxTokens(8192)
            .maxTurns(50)
            .maxBudgetUsd(5.0)
            .initialMessages(messages)
            .abortController(abort)
            .tools(tools)
            .readFileCache(cache)
            .build();

        assertEquals("claude-opus-4-20250514", config.model());
        assertEquals("You are helpful.", config.systemPrompt());
        assertEquals(8192, config.maxTokens());
        assertEquals(50, config.maxTurns());
        assertEquals(5.0, config.maxBudgetUsd());
        assertEquals(1, config.initialMessages().size());
        assertSame(abort, config.abortController());
        assertEquals(2, config.tools().size());
        assertEquals("content", config.readFileCache().get("file.txt"));
    }

    @Test
    void initialMessagesAreDefensivelyCopied() {
        var messages = new java.util.ArrayList<Message>();
        messages.add(new UserMessage("u1", MessageContent.ofText("hello")));

        var config = QueryEngineConfig.builder()
            .llmClient(NOOP_CLIENT)
            .initialMessages(messages)
            .build();

        messages.add(new UserMessage("u2", MessageContent.ofText("world")));
        assertEquals(1, config.initialMessages().size());
    }

    @Test
    void setUserSpecifiedModelUpdatesModel() {
        var config = QueryEngineConfig.builder()
            .llmClient(NOOP_CLIENT)
            .model("original-model")
            .build();

        config.setUserSpecifiedModel("new-model");
        assertEquals("new-model", config.model());
    }

    @Test
    void initialMessagesListIsUnmodifiable() {
        var config = QueryEngineConfig.builder()
            .llmClient(NOOP_CLIENT)
            .build();

        assertThrows(UnsupportedOperationException.class, () ->
            config.initialMessages().add(new UserMessage("u1", MessageContent.ofText("x")))
        );
    }
}
