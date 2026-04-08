package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage statistics from an API response.
 */
public record Usage(
    @JsonProperty("input_tokens") long inputTokens,
    @JsonProperty("output_tokens") long outputTokens,
    @JsonProperty("cache_creation_input_tokens") long cacheCreationInputTokens,
    @JsonProperty("cache_read_input_tokens") long cacheReadInputTokens
) {

    /** Empty usage constant — zero tokens across all fields. */
    public static final Usage EMPTY = new Usage(0, 0, 0, 0);

    @JsonCreator
    public Usage {
    }

    /**
     * Adds another Usage to this one, returning a new Usage with accumulated values.
     */
    public Usage add(Usage other) {
        if (other == null) {
            return this;
        }
        return new Usage(
            this.inputTokens + other.inputTokens,
            this.outputTokens + other.outputTokens,
            this.cacheCreationInputTokens + other.cacheCreationInputTokens,
            this.cacheReadInputTokens + other.cacheReadInputTokens
        );
    }

    /**
     * Returns the total number of tokens (input + output).
     */
    public long totalTokens() {
        return inputTokens + outputTokens;
    }
}
