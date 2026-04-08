package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A text content block.
 */
public record TextBlock(
    @JsonProperty("text") String text
) implements ContentBlock {

    @JsonCreator
    public TextBlock {
    }
}
