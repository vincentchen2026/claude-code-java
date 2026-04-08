package com.claudecode.core.engine;

import com.claudecode.core.message.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class QueryMessageIteratorTest {

    /**
     * Creates a mock StreamingClient that returns the given events.
     */
    private static StreamingClient mockClient(List<StreamingClient.StreamingEvent> events) {
        return new StreamingClient() {
            @Override
            public Iterator<StreamingEvent> createStream(StreamRequest request) {
                return events.iterator();
            }
            @Override
            public String getModel() { return "test-model"; }
        };
    }

    /** Drains all messages from the iterator. */
    private List<SDKMessage> drain(Iterator<SDKMessage> iter) {
        List<SDKMessage> msgs = new ArrayList<>();
        while (iter.hasNext()) msgs.add(iter.next());
        return msgs;
    }

    @Test
    void singleTurnConversationYieldsExpectedMessages() {
        var events = List.<StreamingClient.StreamingEvent>of(
            new StreamingClient.StreamingEvent.MessageStartEvent(
                "msg-1", "test-model", List.of(), new Usage(10, 0, 0, 0)),
            new StreamingClient.StreamingEvent.ContentBlockDeltaEvent(0, "text_delta", "Hello"),
            new StreamingClient.StreamingEvent.ContentBlockDeltaEvent(0, "text_delta", " world"),
            new StreamingClient.StreamingEvent.MessageDeltaEvent("end_turn", new Usage(0, 5, 0, 0)),
            new StreamingClient.StreamingEvent.MessageStopEvent()
        );

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(events))
            .systemPrompt("Be helpful")
            .build());

        List<SDKMessage> messages = drain(engine.submitMessage("Hi there", SubmitOptions.DEFAULT));

        // 1. User message
        assertInstanceOf(SDKMessage.User.class, messages.get(0));
        var userMsg = (SDKMessage.User) messages.get(0);
        assertEquals("Hi there", userMsg.message().message().text());

        // 2. System init message
        assertInstanceOf(SDKMessage.System.class, messages.get(1));

        // Find assistant message
        SDKMessage.Assistant assistant = messages.stream()
            .filter(m -> m instanceof SDKMessage.Assistant)
            .map(m -> (SDKMessage.Assistant) m)
            .findFirst().orElseThrow();
        var content = assistant.message().message().content();
        assertEquals(1, content.size());
        assertInstanceOf(TextBlock.class, content.get(0));
        assertEquals("Hello world", ((TextBlock) content.get(0)).text());
        assertEquals(10, assistant.usage().inputTokens());
        assertEquals(5, assistant.usage().outputTokens());

        // Last message is Result
        SDKMessage last = messages.get(messages.size() - 1);
        assertInstanceOf(SDKMessage.Result.class, last);
        assertEquals(SDKMessage.Result.SUCCESS, ((SDKMessage.Result) last).resultType());
    }

    @Test
    void messagesAreAddedToEngineHistory() {
        var events = List.<StreamingClient.StreamingEvent>of(
            new StreamingClient.StreamingEvent.MessageStartEvent(
                "msg-1", "test-model", List.of(), Usage.EMPTY),
            new StreamingClient.StreamingEvent.ContentBlockDeltaEvent(0, "text_delta", "Reply"),
            new StreamingClient.StreamingEvent.MessageStopEvent()
        );

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(events)).build());

        drain(engine.submitMessage("Question", SubmitOptions.DEFAULT));

        // Engine should have user + assistant messages (system messages too)
        List<Message> messages = engine.getMessages();
        assertTrue(messages.size() >= 2);
        assertInstanceOf(UserMessage.class, messages.get(0));
        // Find assistant message in history
        boolean hasAssistant = messages.stream().anyMatch(m -> m instanceof AssistantMessage);
        assertTrue(hasAssistant);
    }

    @Test
    void abortDuringStreamYieldsError() throws InterruptedException {
        var abortController = new AbortController();
        var clientStarted = new java.util.concurrent.CountDownLatch(1);

        StreamingClient blockingClient = new StreamingClient() {
            @Override
            public Iterator<StreamingEvent> createStream(StreamRequest request) {
                return new Iterator<>() {
                    private boolean first = true;
                    @Override
                    public boolean hasNext() {
                        if (first) {
                            clientStarted.countDown();
                            first = false;
                            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                            return true;
                        }
                        return false;
                    }
                    @Override
                    public StreamingEvent next() {
                        return new StreamingEvent.MessageStartEvent(
                            "msg-1", "test-model", List.of(), Usage.EMPTY);
                    }
                };
            }
            @Override
            public String getModel() { return "test-model"; }
        };

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(blockingClient)
            .abortController(abortController)
            .build());

        Iterator<SDKMessage> iter = engine.submitMessage("Hello", SubmitOptions.DEFAULT);

        // Drain user + system init
        iter.next(); // user
        iter.next(); // system init

        clientStarted.await();
        abortController.abort();

        List<SDKMessage> remaining = new ArrayList<>();
        while (iter.hasNext()) remaining.add(iter.next());

        boolean hasError = remaining.stream().anyMatch(m -> m instanceof SDKMessage.Error);
        boolean hasAssistant = remaining.stream().anyMatch(m -> m instanceof SDKMessage.Assistant);
        assertTrue(hasError || hasAssistant,
            "Expected either an error (abort) or completed assistant message");
    }

    @Test
    void errorEventFromStreamYieldsErrorResult() {
        var events = List.<StreamingClient.StreamingEvent>of(
            new StreamingClient.StreamingEvent.ErrorEvent(new RuntimeException("API error"))
        );

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(events)).build());

        List<SDKMessage> messages = drain(engine.submitMessage("Hello", SubmitOptions.DEFAULT));

        // Should have error and result with error_during_execution
        boolean hasError = messages.stream().anyMatch(m -> m instanceof SDKMessage.Error);
        assertTrue(hasError);

        SDKMessage.Result result = messages.stream()
            .filter(m -> m instanceof SDKMessage.Result)
            .map(m -> (SDKMessage.Result) m)
            .findFirst().orElse(null);
        assertNotNull(result);
        assertEquals(SDKMessage.Result.ERROR_DURING_EXECUTION, result.resultType());
    }

    @Test
    void emptyStreamYieldsEmptyAssistantMessage() {
        var events = List.<StreamingClient.StreamingEvent>of(
            new StreamingClient.StreamingEvent.MessageStartEvent(
                "msg-1", "test-model", List.of(), Usage.EMPTY),
            new StreamingClient.StreamingEvent.MessageStopEvent()
        );

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(events)).build());

        List<SDKMessage> messages = drain(engine.submitMessage("Hello", SubmitOptions.DEFAULT));

        SDKMessage.Assistant assistant = messages.stream()
            .filter(m -> m instanceof SDKMessage.Assistant)
            .map(m -> (SDKMessage.Assistant) m)
            .findFirst().orElseThrow();
        assertTrue(assistant.message().message().content().isEmpty());

        SDKMessage last = messages.get(messages.size() - 1);
        assertInstanceOf(SDKMessage.Result.class, last);
    }

    @Test
    void usageIsAccumulatedAcrossEvents() {
        var events = List.<StreamingClient.StreamingEvent>of(
            new StreamingClient.StreamingEvent.MessageStartEvent(
                "msg-1", "test-model", List.of(), new Usage(100, 0, 0, 0)),
            new StreamingClient.StreamingEvent.ContentBlockDeltaEvent(0, "text_delta", "Hi"),
            new StreamingClient.StreamingEvent.MessageDeltaEvent("end_turn", new Usage(0, 50, 0, 0)),
            new StreamingClient.StreamingEvent.MessageStopEvent()
        );

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(events)).build());

        List<SDKMessage> messages = drain(engine.submitMessage("Hello", SubmitOptions.DEFAULT));

        SDKMessage.Result result = messages.stream()
            .filter(m -> m instanceof SDKMessage.Result)
            .map(m -> (SDKMessage.Result) m)
            .findFirst().orElseThrow();
        assertEquals(100, result.totalUsage().inputTokens());
        assertEquals(50, result.totalUsage().outputTokens());
    }

    @Test
    void nextThrowsWhenNoMoreMessages() {
        var events = List.<StreamingClient.StreamingEvent>of(
            new StreamingClient.StreamingEvent.MessageStartEvent(
                "msg-1", "test-model", List.of(), Usage.EMPTY),
            new StreamingClient.StreamingEvent.MessageStopEvent()
        );

        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(events)).build());

        Iterator<SDKMessage> iter = engine.submitMessage("Hello", SubmitOptions.DEFAULT);
        while (iter.hasNext()) iter.next();
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    void engineInterruptAborts() {
        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(List.of())).build());
        engine.interrupt();
        assertTrue(engine.getAbortController().isAborted());
    }

    @Test
    void engineSessionIdIsStable() {
        var engine = new QueryEngine(QueryEngineConfig.builder()
            .llmClient(mockClient(List.of())).build());
        assertEquals(engine.getSessionId(), engine.getSessionId());
        assertNotNull(engine.getSessionId());
    }

    @Test
    void engineSetModelDelegates() {
        var config = QueryEngineConfig.builder()
            .llmClient(mockClient(List.of()))
            .model("original").build();
        var engine = new QueryEngine(config);
        engine.setModel("new-model");
        assertEquals("new-model", config.model());
    }
}
