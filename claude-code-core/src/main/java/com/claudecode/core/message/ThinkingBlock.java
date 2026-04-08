package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A thinking content block — extended thinking output from the model.
 */
public record ThinkingBlock(
    @JsonProperty("thinking") String thinking
) implements ContentBlock {

    @JsonCreator
    public ThinkingBlock {
    }
}
