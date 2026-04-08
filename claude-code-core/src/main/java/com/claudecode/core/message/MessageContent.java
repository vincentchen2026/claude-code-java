package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Content of a user message — either a simple string or a list of content blocks.
 */
public record MessageContent(
    @JsonProperty("text") String text,
    @JsonProperty("blocks") List<ContentBlock> blocks
) {

    @JsonCreator
    public MessageContent {
    }
    /**
     * Creates a text-only message content.
     */
    public static MessageContent ofText(String text) {
        return new MessageContent(text, null);
    }

    /**
     * Creates a block-based message content.
     */
    public static MessageContent ofBlocks(List<ContentBlock> blocks) {
        return new MessageContent(null, blocks);
    }

    /**
     * Creates a tool result message content.
     */
    public static MessageContent ofToolResult(String toolUseId, List<ContentBlock> content, boolean isError) {
        ToolResultBlock resultBlock = new ToolResultBlock(toolUseId, content, isError);
        return new MessageContent(null, List.of(resultBlock));
    }

    /**
     * Returns true if this content is text-only.
     */
    @JsonIgnore
    public boolean isText() {
        return text != null;
    }
}
