package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * An attachment message carrying additional context (e.g., post-compact file re-reads).
 */
public record AttachmentMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("content") String content,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public AttachmentMessage {
    }

    public AttachmentMessage(String uuid, String content) {
        this(uuid, content, null, Instant.now());
    }

    @Override
    public String type() {
        return "attachment";
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
