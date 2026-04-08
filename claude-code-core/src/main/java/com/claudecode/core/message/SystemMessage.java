package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * A system message in the conversation.
 * Subtypes include: "local_command", "compact_boundary", "api_error", etc.
 */
public record SystemMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("subtype") String subtype,
    @JsonProperty("level") String level,
    @JsonProperty("content") String content,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public SystemMessage {
    }

    /**
     * Convenience constructor.
     */
    public SystemMessage(String uuid, String subtype, String level, String content) {
        this(uuid, subtype, level, content, null, Instant.now());
    }

    @Override
    public String type() {
        return "system";
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
