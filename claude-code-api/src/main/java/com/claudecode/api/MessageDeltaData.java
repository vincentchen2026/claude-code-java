package com.claudecode.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message-level delta data (stop reason update).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageDeltaData(
        @JsonProperty("stop_reason") String stopReason,
        @JsonProperty("stop_sequence") String stopSequence
) {
    @JsonCreator
    public MessageDeltaData {
    }
}
