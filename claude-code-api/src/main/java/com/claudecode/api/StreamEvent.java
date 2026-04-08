package com.claudecode.api;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.Usage;

/**
 * Unified stream event types (SDK-agnostic).
 * Maps to Anthropic SSE event types: message_start, content_block_start,
 * content_block_delta, content_block_stop, message_delta, message_stop.
 */
public sealed interface StreamEvent permits
        StreamEvent.MessageStart,
        StreamEvent.ContentBlockStart,
        StreamEvent.ContentBlockDelta,
        StreamEvent.ContentBlockStop,
        StreamEvent.MessageDelta,
        StreamEvent.MessageStop,
        StreamEvent.Error,
        StreamEvent.Ping {

    /** Fired when a new message begins. Contains the initial ApiMessage shell. */
    record MessageStart(ApiMessage message) implements StreamEvent {}

    /** Fired when a new content block begins within the message. */
    record ContentBlockStart(int index, ContentBlock contentBlock) implements StreamEvent {}

    /** Fired when a content block receives incremental data. */
    record ContentBlockDelta(int index, Delta delta) implements StreamEvent {}

    /** Fired when a content block is complete. */
    record ContentBlockStop(int index) implements StreamEvent {}

    /** Fired when the message-level metadata updates (stop reason, usage). */
    record MessageDelta(MessageDeltaData delta, Usage usage) implements StreamEvent {}

    /** Fired when the entire message is complete. */
    record MessageStop() implements StreamEvent {}

    /** Fired when an error occurs during streaming. */
    record Error(ApiException exception) implements StreamEvent {}

    /** Keep-alive ping event. */
    record Ping() implements StreamEvent {}
}
