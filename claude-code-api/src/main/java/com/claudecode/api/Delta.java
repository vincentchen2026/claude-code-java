package com.claudecode.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Delta types for streaming content block updates.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Delta.TextDelta.class, name = "text_delta"),
        @JsonSubTypes.Type(value = Delta.InputJsonDelta.class, name = "input_json_delta"),
        @JsonSubTypes.Type(value = Delta.ThinkingDelta.class, name = "thinking_delta")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface Delta permits Delta.TextDelta, Delta.InputJsonDelta, Delta.ThinkingDelta {

    String type();

    /**
     * Incremental text content.
     */
    record TextDelta(
            @JsonProperty("text") String text
    ) implements Delta {
        @JsonCreator
        public TextDelta {}

        @Override
        public String type() { return "text_delta"; }
    }

    /**
     * Incremental JSON input for tool use blocks.
     */
    record InputJsonDelta(
            @JsonProperty("partial_json") String partialJson
    ) implements Delta {
        @JsonCreator
        public InputJsonDelta {}

        @Override
        public String type() { return "input_json_delta"; }
    }

    /**
     * Incremental thinking content.
     */
    record ThinkingDelta(
            @JsonProperty("thinking") String thinking
    ) implements Delta {
        @JsonCreator
        public ThinkingDelta {}

        @Override
        public String type() { return "thinking_delta"; }
    }
}
