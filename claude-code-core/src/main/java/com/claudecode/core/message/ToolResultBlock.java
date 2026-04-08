package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A tool result content block — the result of executing a tool.
 */
public record ToolResultBlock(
    @JsonProperty("tool_use_id") String toolUseId,
    @JsonProperty("content") List<ContentBlock> content,
    @JsonProperty("is_error") boolean isError
) implements ContentBlock {

    @JsonCreator
    public ToolResultBlock {
    }
}
