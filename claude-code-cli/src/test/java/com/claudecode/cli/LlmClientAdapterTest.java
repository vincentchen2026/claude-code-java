package com.claudecode.cli;

import com.claudecode.api.*;
import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.StreamingClient.StreamingEvent;
import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.Usage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LlmClientAdapter that bridges api LlmClient to core StreamingClient.
 */
class LlmClientAdapterTest {

    @Test
    void testAdapterConvertsStreamEvents() {
        // Create a mock LlmClient that returns known StreamEvents
        LlmClient mockLlm = new LlmClient() {
            @Override
            public Iterator<StreamEvent> createMessageStream(CreateMessageRequest request) {
                List<StreamEvent> events = List.of(
                    new StreamEvent.MessageStart(
                        ApiMessage.builder()
                            .id("msg_001")
                            .model("test-model")
                            .usage(new Usage(10, 0, 0, 0))
                            .build()
                    ),
                    new StreamEvent.ContentBlockStart(0, new TextBlock("")),
                    new StreamEvent.ContentBlockDelta(0, new Delta.TextDelta("Hello ")),
                    new StreamEvent.ContentBlockDelta(0, new Delta.TextDelta("world!")),
                    new StreamEvent.ContentBlockStop(0),
                    new StreamEvent.MessageDelta(
                        new MessageDeltaData("end_turn", null),
                        new Usage(0, 15, 0, 0)
                    ),
                    new StreamEvent.MessageStop()
                );
                return events.iterator();
            }

            @Override
            public ApiMessage createMessage(CreateMessageRequest request) {
                return null;
            }

            @Override
            public String getModel() {
                return "test-model";
            }
        };

        LlmClientAdapter adapter = new LlmClientAdapter(mockLlm);
        assertEquals("test-model", adapter.getModel());

        StreamingClient.StreamRequest request = new StreamingClient.StreamRequest(
            "test-model", 100, "system", List.of(), true
        );

        Iterator<StreamingEvent> events = adapter.createStream(request);
        List<StreamingEvent> collected = new ArrayList<>();
        while (events.hasNext()) {
            collected.add(events.next());
        }

        // Should have: MessageStart, ContentBlockStart, 2x ContentBlockDelta, ContentBlockStop, MessageDelta, MessageStop
        // (ContentBlockStart and ContentBlockStop are now forwarded, only Ping is filtered)
        assertEquals(7, collected.size());

        assertInstanceOf(StreamingEvent.MessageStartEvent.class, collected.get(0));
        StreamingEvent.MessageStartEvent start = (StreamingEvent.MessageStartEvent) collected.get(0);
        assertEquals("msg_001", start.messageId());

        assertInstanceOf(StreamingEvent.ContentBlockStartEvent.class, collected.get(1));
        StreamingEvent.ContentBlockStartEvent cbStart = (StreamingEvent.ContentBlockStartEvent) collected.get(1);
        assertEquals(0, cbStart.index());
        assertEquals("text", cbStart.type());

        assertInstanceOf(StreamingEvent.ContentBlockDeltaEvent.class, collected.get(2));
        StreamingEvent.ContentBlockDeltaEvent delta1 = (StreamingEvent.ContentBlockDeltaEvent) collected.get(2);
        assertEquals("Hello ", delta1.deltaText());

        assertInstanceOf(StreamingEvent.ContentBlockDeltaEvent.class, collected.get(3));
        StreamingEvent.ContentBlockDeltaEvent delta2 = (StreamingEvent.ContentBlockDeltaEvent) collected.get(3);
        assertEquals("world!", delta2.deltaText());

        assertInstanceOf(StreamingEvent.ContentBlockStopEvent.class, collected.get(4));
        StreamingEvent.ContentBlockStopEvent cbStop = (StreamingEvent.ContentBlockStopEvent) collected.get(4);
        assertEquals(0, cbStop.index());

        assertInstanceOf(StreamingEvent.MessageDeltaEvent.class, collected.get(5));
        assertInstanceOf(StreamingEvent.MessageStopEvent.class, collected.get(6));
    }

    @Test
    void testAdapterPassesRequestParameters() {
        // Verify that the adapter correctly converts StreamRequest to CreateMessageRequest
        final CreateMessageRequest[] capturedRequest = {null};

        LlmClient capturingClient = new LlmClient() {
            @Override
            public Iterator<StreamEvent> createMessageStream(CreateMessageRequest request) {
                capturedRequest[0] = request;
                return List.<StreamEvent>of(new StreamEvent.MessageStop()).iterator();
            }

            @Override
            public ApiMessage createMessage(CreateMessageRequest request) {
                return null;
            }

            @Override
            public String getModel() {
                return "test-model";
            }
        };

        LlmClientAdapter adapter = new LlmClientAdapter(capturingClient);

        StreamingClient.StreamRequest request = new StreamingClient.StreamRequest(
            "claude-sonnet-4-20250514",
            8192,
            "You are helpful",
            List.of(
                new StreamingClient.StreamRequest.RequestMessage("user", "Hello"),
                new StreamingClient.StreamRequest.RequestMessage("assistant", "Hi there")
            ),
            true
        );

        // Consume the iterator
        Iterator<StreamingEvent> events = adapter.createStream(request);
        while (events.hasNext()) events.next();

        assertNotNull(capturedRequest[0]);
        assertEquals("claude-sonnet-4-20250514", capturedRequest[0].model());
        assertEquals(8192, capturedRequest[0].maxTokens());
        assertEquals("You are helpful", capturedRequest[0].systemPrompt());
        assertEquals(2, capturedRequest[0].messages().size());
        assertEquals("user", capturedRequest[0].messages().get(0).role());
        assertEquals("Hello", capturedRequest[0].messages().get(0).content());
    }
}
