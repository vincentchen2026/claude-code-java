package com.claudecode.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SdkMessageAdapter {

    private final ObjectMapper objectMapper;

    public SdkMessageAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SdkMessageAdapter() {
        this(new ObjectMapper());
    }

    public JsonNode toSdkFormat(InboundMessage message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", message.id());
        node.put("sessionId", message.sessionId());
        node.put("timestamp", message.timestamp().toString());
        node.put("type", getTypeString(message));

        switch (message) {
            case InboundTextMessage m -> {
                node.put("text", m.text());
            }
            case InboundToolCall m -> {
                node.put("toolName", m.toolName());
                node.put("arguments", m.arguments());
            }
            case InboundToolResult m -> {
                node.put("callId", m.callId());
                node.put("result", m.result());
                node.put("isError", m.isError());
            }
            case InboundError m -> {
                node.put("errorCode", m.errorCode());
                node.put("errorMessage", m.errorMessage());
            }
            case InboundPing m -> {
            }
            case InboundPong m -> {
            }
        }

        return node;
    }

    public JsonNode toSdkFormat(OutboundMessage message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", message.id());
        node.put("sessionId", message.sessionId());
        node.put("timestamp", message.timestamp().toString());
        node.put("type", getOutboundTypeString(message));

        switch (message) {
            case OutboundTextMessage m -> {
                node.put("text", m.text());
            }
            case OutboundToolCall m -> {
                node.put("toolName", m.toolName());
                node.put("arguments", m.arguments());
            }
            case OutboundToolResult m -> {
                node.put("callId", m.callId());
                node.put("result", m.result());
            }
            case OutboundError m -> {
                node.put("errorCode", m.errorCode());
                node.put("errorMessage", m.errorMessage());
            }
        }

        return node;
    }

    private String getTypeString(InboundMessage message) {
        if (message instanceof InboundTextMessage) return "text";
        if (message instanceof InboundToolCall) return "tool_call";
        if (message instanceof InboundToolResult) return "tool_result";
        if (message instanceof InboundError) return "error";
        if (message instanceof InboundPing) return "ping";
        if (message instanceof InboundPong) return "pong";
        return "unknown";
    }

    private String getOutboundTypeString(OutboundMessage message) {
        if (message instanceof OutboundTextMessage) return "text";
        if (message instanceof OutboundToolCall) return "tool_call";
        if (message instanceof OutboundToolResult) return "tool_result";
        if (message instanceof OutboundError) return "error";
        return "unknown";
    }

    public sealed interface OutboundMessage permits OutboundTextMessage, OutboundToolCall, OutboundToolResult, OutboundError {
        String id();
        String sessionId();
        Instant timestamp();
    }

    public record OutboundTextMessage(
        String id,
        String sessionId,
        Instant timestamp,
        String text
    ) implements OutboundMessage {}

    public record OutboundToolCall(
        String id,
        String sessionId,
        Instant timestamp,
        String toolName,
        JsonNode arguments
    ) implements OutboundMessage {}

    public record OutboundToolResult(
        String id,
        String sessionId,
        Instant timestamp,
        String callId,
        String result
    ) implements OutboundMessage {}

    public record OutboundError(
        String id,
        String sessionId,
        Instant timestamp,
        String errorCode,
        String errorMessage
    ) implements OutboundMessage {}
}