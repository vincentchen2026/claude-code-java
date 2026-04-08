package com.claudecode.api;

import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.ToolUseBlock;
import com.claudecode.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for stream event parsing — verifies that SSE events are correctly
 * adapted to our StreamEvent interface (CP-1: API protocol correctness).
 */
class StreamEventParsingTest {

    @Test
    void parsesCompleteMessageStream() {
        String sse = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_01","type":"message","role":"assistant","content":[],"model":"claude-sonnet-4-20250514","stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":25,"output_tokens":1,"cache_creation_input_tokens":0,"cache_read_input_tokens":0}}}
                
                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
                
                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}
                
                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" world"}}
                
                event: content_block_stop
                data: {"type":"content_block_stop","index":0}
                
                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":12,"input_tokens":0,"cache_creation_input_tokens":0,"cache_read_input_tokens":0}}
                
                event: message_stop
                data: {"type":"message_stop"}
                
                """;

        List<StreamEvent> events = parseStreamEvents(sse);

        assertEquals(7, events.size());

        // message_start
        assertInstanceOf(StreamEvent.MessageStart.class, events.get(0));
        StreamEvent.MessageStart start = (StreamEvent.MessageStart) events.get(0);
        assertEquals("msg_01", start.message().id());
        assertEquals("assistant", start.message().role());

        // content_block_start
        assertInstanceOf(StreamEvent.ContentBlockStart.class, events.get(1));
        StreamEvent.ContentBlockStart blockStart = (StreamEvent.ContentBlockStart) events.get(1);
        assertEquals(0, blockStart.index());
        assertInstanceOf(TextBlock.class, blockStart.contentBlock());

        // content_block_delta (text)
        assertInstanceOf(StreamEvent.ContentBlockDelta.class, events.get(2));
        StreamEvent.ContentBlockDelta delta1 = (StreamEvent.ContentBlockDelta) events.get(2);
        assertEquals(0, delta1.index());
        assertInstanceOf(Delta.TextDelta.class, delta1.delta());
        assertEquals("Hello", ((Delta.TextDelta) delta1.delta()).text());

        // second delta
        assertInstanceOf(StreamEvent.ContentBlockDelta.class, events.get(3));
        StreamEvent.ContentBlockDelta delta2 = (StreamEvent.ContentBlockDelta) events.get(3);
        assertEquals(" world", ((Delta.TextDelta) delta2.delta()).text());

        // content_block_stop
        assertInstanceOf(StreamEvent.ContentBlockStop.class, events.get(4));
        assertEquals(0, ((StreamEvent.ContentBlockStop) events.get(4)).index());

        // message_delta
        assertInstanceOf(StreamEvent.MessageDelta.class, events.get(5));
        StreamEvent.MessageDelta msgDelta = (StreamEvent.MessageDelta) events.get(5);
        assertEquals("end_turn", msgDelta.delta().stopReason());

        // message_stop
        assertInstanceOf(StreamEvent.MessageStop.class, events.get(6));
    }

    @Test
    void parsesToolUseStream() {
        String sse = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_02","type":"message","role":"assistant","content":[],"model":"claude-sonnet-4-20250514","stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":50,"output_tokens":1,"cache_creation_input_tokens":0,"cache_read_input_tokens":0}}}
                
                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"toolu_01","name":"bash","input":{}}}
                
                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\\"command\\": \\"ls\\"}"}}
                
                event: content_block_stop
                data: {"type":"content_block_stop","index":0}
                
                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"tool_use","stop_sequence":null},"usage":{"output_tokens":30,"input_tokens":0,"cache_creation_input_tokens":0,"cache_read_input_tokens":0}}
                
                event: message_stop
                data: {"type":"message_stop"}
                
                """;

        List<StreamEvent> events = parseStreamEvents(sse);

        assertEquals(6, events.size());

        // content_block_start with tool_use
        assertInstanceOf(StreamEvent.ContentBlockStart.class, events.get(1));
        StreamEvent.ContentBlockStart blockStart = (StreamEvent.ContentBlockStart) events.get(1);
        assertInstanceOf(ToolUseBlock.class, blockStart.contentBlock());
        ToolUseBlock toolUse = (ToolUseBlock) blockStart.contentBlock();
        assertEquals("toolu_01", toolUse.id());
        assertEquals("bash", toolUse.name());

        // input_json_delta
        assertInstanceOf(StreamEvent.ContentBlockDelta.class, events.get(2));
        StreamEvent.ContentBlockDelta delta = (StreamEvent.ContentBlockDelta) events.get(2);
        assertInstanceOf(Delta.InputJsonDelta.class, delta.delta());

        // message_delta with tool_use stop reason
        assertInstanceOf(StreamEvent.MessageDelta.class, events.get(4));
        assertEquals("tool_use", ((StreamEvent.MessageDelta) events.get(4)).delta().stopReason());
    }

    @Test
    void parsesPingEvents() {
        String sse = """
                event: ping
                data: {}
                
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_03","type":"message","role":"assistant","content":[],"model":"test","stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":1,"cache_creation_input_tokens":0,"cache_read_input_tokens":0}}}
                
                event: message_stop
                data: {"type":"message_stop"}
                
                """;

        List<StreamEvent> events = parseStreamEvents(sse);

        assertEquals(3, events.size());
        assertInstanceOf(StreamEvent.Ping.class, events.get(0));
        assertInstanceOf(StreamEvent.MessageStart.class, events.get(1));
        assertInstanceOf(StreamEvent.MessageStop.class, events.get(2));
    }

    private List<StreamEvent> parseStreamEvents(String sseText) {
        ByteArrayInputStream input = new ByteArrayInputStream(
                sseText.getBytes(StandardCharsets.UTF_8));
        SseParser sseParser = new SseParser(input);
        AnthropicSdkClient.StreamEventIterator iterator =
                new AnthropicSdkClient.StreamEventIterator(sseParser, JsonUtils.getMapper());

        List<StreamEvent> events = new ArrayList<>();
        while (iterator.hasNext()) {
            events.add(iterator.next());
        }
        return events;
    }
}
