package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * A summary message for tool use operations.
 */
public record ToolUseSummaryMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("toolName") String toolName,
    @JsonProperty("toolUseId") String toolUseId,
    @JsonProperty("summary") String summary,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public ToolUseSummaryMessage {
    }

    public ToolUseSummaryMessage(String uuid, String toolName, String toolUseId, String summary) {
        this(uuid, toolName, toolUseId, summary, null, Instant.now());
    }

    @Override
    public String type() {
        return "tool_use_summary";
    }

    @Override
    public Optional<String> parentUuid() {
        return Optional.ofNullable(parentUuidValue);
    }

    @Override
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestampValue);
    }
}
