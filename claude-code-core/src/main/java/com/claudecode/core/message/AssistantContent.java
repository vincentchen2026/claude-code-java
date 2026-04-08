package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Content of an assistant message — a list of content blocks with an optional API message ID.
 */
public record AssistantContent(
    @JsonProperty("id") String id,
    @JsonProperty("content") List<ContentBlock> content
) {

    @JsonCreator
    public AssistantContent {
    }
    /**
     * Creates assistant content with the given blocks.
     */
    public static AssistantContent of(List<ContentBlock> content) {
        return new AssistantContent(null, content);
    }

    /**
     * Creates assistant content with an API message ID and blocks.
     */
    public static AssistantContent of(String id, List<ContentBlock> content) {
        return new AssistantContent(id, content);
    }
}
