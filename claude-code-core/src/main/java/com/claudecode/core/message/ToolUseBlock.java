package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A tool use content block — represents the model requesting a tool call.
 */
public record ToolUseBlock(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("input") JsonNode input
) implements ContentBlock {

    @JsonCreator
    public ToolUseBlock {
    }
}
