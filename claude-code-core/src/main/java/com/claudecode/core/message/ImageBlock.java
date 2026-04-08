package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * An image content block.
 */
public record ImageBlock(
    @JsonProperty("source") JsonNode source
) implements ContentBlock {

    @JsonCreator
    public ImageBlock {
    }
}
