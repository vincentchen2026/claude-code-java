package com.claudecode.api;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.Usage;
import com.claudecode.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiMessage deserialization and builder.
 */
class ApiMessageTest {

    @Test
    void builderCreatesValidMessage() {
        ApiMessage message = ApiMessage.builder()
                .id("msg_123")
                .model("claude-sonnet-4-20250514")
                .role("assistant")
                .content(List.of(new TextBlock("Hello!")))
                .stopReason("end_turn")
                .usage(new Usage(100, 50, 0, 0))
                .build();

        assertEquals("msg_123", message.id());
        assertEquals("assistant", message.role());
        assertEquals("claude-sonnet-4-20250514", message.model());
        assertEquals("end_turn", message.stopReason());
        assertEquals(1, message.content().size());
        assertEquals(100, message.usage().inputTokens());
    }

    @Test
    void deserializesFromJson() {
        String json = """
                {
                    "id": "msg_abc",
                    "type": "message",
                    "role": "assistant",
                    "content": [{"type": "text", "text": "Hello world"}],
                    "model": "claude-sonnet-4-20250514",
                    "stop_reason": "end_turn",
                    "usage": {"input_tokens": 200, "output_tokens": 100, "cache_creation_input_tokens": 0, "cache_read_input_tokens": 0}
                }
                """;

        ApiMessage message = JsonUtils.fromJson(json, ApiMessage.class);

        assertEquals("msg_abc", message.id());
        assertEquals("message", message.type());
        assertEquals("assistant", message.role());
        assertEquals("claude-sonnet-4-20250514", message.model());
        assertEquals("end_turn", message.stopReason());
        assertEquals(200, message.usage().inputTokens());
        assertEquals(100, message.usage().outputTokens());
        assertFalse(message.content().isEmpty());
    }

    @Test
    void serializesToJson() {
        ApiMessage message = ApiMessage.builder()
                .id("msg_test")
                .model("claude-sonnet-4-20250514")
                .content(List.of(new TextBlock("Test response")))
                .stopReason("end_turn")
                .usage(new Usage(50, 25, 0, 0))
                .build();

        String json = JsonUtils.toJson(message);
        JsonNode node = JsonUtils.parseTree(json);

        assertEquals("msg_test", node.get("id").asText());
        assertEquals("assistant", node.get("role").asText());
        assertEquals("end_turn", node.get("stop_reason").asText());
    }

    @Test
    void handlesUnknownFieldsGracefully() {
        String json = """
                {
                    "id": "msg_unknown",
                    "type": "message",
                    "role": "assistant",
                    "content": [],
                    "model": "test",
                    "stop_reason": null,
                    "usage": {"input_tokens": 0, "output_tokens": 0, "cache_creation_input_tokens": 0, "cache_read_input_tokens": 0},
                    "some_future_field": "value"
                }
                """;

        // Should not throw — FAIL_ON_UNKNOWN_PROPERTIES is false
        ApiMessage message = JsonUtils.fromJson(json, ApiMessage.class);
        assertEquals("msg_unknown", message.id());
    }
}
