package com.claudecode.api;

import com.claudecode.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Delta types serialization/deserialization.
 */
class DeltaTest {

    @Test
    void textDeltaSerializesCorrectly() {
        Delta.TextDelta delta = new Delta.TextDelta("Hello");
        String json = JsonUtils.toJson(delta);

        assertTrue(json.contains("\"text\":\"Hello\""));
        assertTrue(json.contains("\"type\":\"text_delta\""));
    }

    @Test
    void inputJsonDeltaSerializesCorrectly() {
        Delta.InputJsonDelta delta = new Delta.InputJsonDelta("{\"key\": \"value\"}");
        String json = JsonUtils.toJson(delta);

        assertTrue(json.contains("\"partial_json\""));
        assertTrue(json.contains("\"type\":\"input_json_delta\""));
    }

    @Test
    void thinkingDeltaSerializesCorrectly() {
        Delta.ThinkingDelta delta = new Delta.ThinkingDelta("Let me think...");
        String json = JsonUtils.toJson(delta);

        assertTrue(json.contains("\"thinking\":\"Let me think...\""));
        assertTrue(json.contains("\"type\":\"thinking_delta\""));
    }

    @Test
    void textDeltaDeserializes() {
        String json = """
                {"type": "text_delta", "text": "world"}
                """;
        Delta delta = JsonUtils.fromJson(json, Delta.class);
        assertInstanceOf(Delta.TextDelta.class, delta);
        assertEquals("world", ((Delta.TextDelta) delta).text());
    }

    @Test
    void inputJsonDeltaDeserializes() {
        String json = """
                {"type": "input_json_delta", "partial_json": "{\\"cmd\\": \\"ls\\"}"}
                """;
        Delta delta = JsonUtils.fromJson(json, Delta.class);
        assertInstanceOf(Delta.InputJsonDelta.class, delta);
    }

    @Test
    void thinkingDeltaDeserializes() {
        String json = """
                {"type": "thinking_delta", "thinking": "reasoning..."}
                """;
        Delta delta = JsonUtils.fromJson(json, Delta.class);
        assertInstanceOf(Delta.ThinkingDelta.class, delta);
        assertEquals("reasoning...", ((Delta.ThinkingDelta) delta).thinking());
    }
}
