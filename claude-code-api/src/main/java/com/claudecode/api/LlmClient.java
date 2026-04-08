package com.claudecode.api;

import java.util.Iterator;

/**
 * Unified LLM client interface.
 * Supports multiple backends through the adapter pattern.
 */
public interface LlmClient {

    /**
     * Creates a streaming message, returning an iterator of stream events.
     */
    Iterator<StreamEvent> createMessageStream(CreateMessageRequest request);

    /**
     * Creates a non-streaming message (blocks until complete).
     */
    ApiMessage createMessage(CreateMessageRequest request);

    /**
     * Returns the current model name.
     */
    String getModel();
}
