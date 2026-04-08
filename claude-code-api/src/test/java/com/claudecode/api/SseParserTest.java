package com.claudecode.api;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SSE stream parsing.
 */
class SseParserTest {

    @Test
    void parsesSimpleEvent() {
        String sse = """
                event: message_start
                data: {"message": {}}
                
                """;

        List<SseParser.SseEvent> events = parseAll(sse);

        assertEquals(1, events.size());
        assertEquals("message_start", events.get(0).event());
        assertEquals("{\"message\": {}}", events.get(0).data());
    }

    @Test
    void parsesMultipleEvents() {
        String sse = """
                event: message_start
                data: {"message": {"id": "msg_1"}}
                
                event: content_block_start
                data: {"index": 0, "content_block": {"type": "text", "text": ""}}
                
                event: content_block_delta
                data: {"index": 0, "delta": {"type": "text_delta", "text": "Hello"}}
                
                event: content_block_stop
                data: {"index": 0}
                
                event: message_stop
                data: {}
                
                """;

        List<SseParser.SseEvent> events = parseAll(sse);

        assertEquals(5, events.size());
        assertEquals("message_start", events.get(0).event());
        assertEquals("content_block_start", events.get(1).event());
        assertEquals("content_block_delta", events.get(2).event());
        assertEquals("content_block_stop", events.get(3).event());
        assertEquals("message_stop", events.get(4).event());
    }

    @Test
    void parsesPingEvent() {
        String sse = """
                event: ping
                data: {}
                
                """;

        List<SseParser.SseEvent> events = parseAll(sse);

        assertEquals(1, events.size());
        assertEquals("ping", events.get(0).event());
    }

    @Test
    void handlesEventWithoutExplicitType() {
        String sse = """
                data: {"some": "data"}
                
                """;

        List<SseParser.SseEvent> events = parseAll(sse);

        assertEquals(1, events.size());
        assertEquals("message", events.get(0).event()); // default type
    }

    @Test
    void handlesEmptyStream() {
        String sse = "";
        List<SseParser.SseEvent> events = parseAll(sse);
        assertTrue(events.isEmpty());
    }

    @Test
    void handlesMultiLineData() {
        String sse = """
                event: message_start
                data: {"line1": true,
                data:  "line2": true}
                
                """;

        List<SseParser.SseEvent> events = parseAll(sse);

        assertEquals(1, events.size());
        assertTrue(events.get(0).data().contains("line1"));
        assertTrue(events.get(0).data().contains("line2"));
    }

    private List<SseParser.SseEvent> parseAll(String sseText) {
        ByteArrayInputStream input = new ByteArrayInputStream(
                sseText.getBytes(StandardCharsets.UTF_8));
        List<SseParser.SseEvent> events = new ArrayList<>();

        try (SseParser parser = new SseParser(input)) {
            while (parser.hasNext()) {
                events.add(parser.next());
            }
        }
        return events;
    }
}
