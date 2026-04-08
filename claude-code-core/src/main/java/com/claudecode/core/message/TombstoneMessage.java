package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * A tombstone message marking a deleted or replaced message.
 */
public record TombstoneMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("replacedUuid") String replacedUuid,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public TombstoneMessage {
    }

    public TombstoneMessage(String uuid, String replacedUuid) {
        this(uuid, replacedUuid, null, Instant.now());
    }

    @Override
    public String type() {
        return "tombstone";
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
