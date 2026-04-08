package com.claudecode.core.engine;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.Usage;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.List;

/**
 * Abstraction for the LLM streaming client, defined in core to avoid
 * circular dependency between core and api modules.
 * <p>
 * The {@code LlmClient} in claude-code-api implements this interface
 * (or an adapter bridges the two).
 */
public interface StreamingClient {

    /**
     * Creates a streaming message request and returns an iterator of stream events.
     *
     * @param request the message request parameters
     * @return iterator of stream events
     */
    Iterator<StreamingEvent> createStream(StreamRequest request);

    /**
     * Returns the current model name.
     */
    String getModel();

    /**
     * A simplified request record for the streaming client.
     */
    record StreamRequest(
        String model,
        int maxTokens,
        String systemPrompt,
        List<RequestMessage> messages,
        boolean stream,
        List<ToolDef> tools,
        JsonNode jsonSchema
    ) {
        /** Convenience constructor without tools. */
        public StreamRequest(String model, int maxTokens, String systemPrompt,
                             List<RequestMessage> messages, boolean stream) {
            this(model, maxTokens, systemPrompt, messages, stream, List.of(), null);
        }

        /** Convenience constructor with tools but no jsonSchema. */
        public StreamRequest(String model, int maxTokens, String systemPrompt,
                             List<RequestMessage> messages, boolean stream,
                             List<ToolDef> tools) {
            this(model, maxTokens, systemPrompt, messages, stream, tools, null);
        }

        public record RequestMessage(String role, Object content) {}

        /** Tool definition for the API request. */
        public record ToolDef(String name, String description, Object inputSchema) {}
    }

    /**
     * Simplified stream event types used by the engine.
     */
    sealed interface StreamingEvent permits
        StreamingEvent.MessageStartEvent,
        StreamingEvent.ContentBlockStartEvent,
        StreamingEvent.ContentBlockDeltaEvent,
        StreamingEvent.ContentBlockStopEvent,
        StreamingEvent.MessageDeltaEvent,
        StreamingEvent.MessageStopEvent,
        StreamingEvent.ErrorEvent {

        record MessageStartEvent(
            String messageId,
            String model,
            List<ContentBlock> content,
            Usage usage
        ) implements StreamingEvent {}

        /** Fired when a new content block begins (e.g. text, tool_use, thinking). */
        record ContentBlockStartEvent(
            int index,
            String type,
            String id,
            String name
        ) implements StreamingEvent {}

        record ContentBlockDeltaEvent(
            int index,
            String deltaType,
            String deltaText
        ) implements StreamingEvent {}

        /** Fired when a content block is complete. */
        record ContentBlockStopEvent(
            int index
        ) implements StreamingEvent {}

        record MessageDeltaEvent(
            String stopReason,
            Usage usage
        ) implements StreamingEvent {}

        record MessageStopEvent() implements StreamingEvent {}

        record ErrorEvent(Exception exception) implements StreamingEvent {}
    }
}
