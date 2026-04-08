package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A message grouping multiple tool use operations together.
 */
public record GroupedToolUseMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("toolUseIds") List<String> toolUseIds,
    @JsonProperty("toolNames") List<String> toolNames,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public GroupedToolUseMessage {
    }

    public GroupedToolUseMessage(String uuid, List<String> toolUseIds, List<String> toolNames) {
        this(uuid, toolUseIds, toolNames, null, Instant.now());
    }

    @Override
    public String type() {
        return "grouped_tool_use";
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
