package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * A progress message indicating ongoing work.
 */
public record ProgressMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("content") String content,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public ProgressMessage {
    }

    public ProgressMessage(String uuid, String content) {
        this(uuid, content, null, Instant.now());
    }

    @Override
    public String type() {
        return "progress";
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
