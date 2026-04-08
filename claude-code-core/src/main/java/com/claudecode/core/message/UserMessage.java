package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * A user message in the conversation.
 */
public record UserMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("message") MessageContent message,
    @JsonProperty("isMeta") boolean isMeta,
    @JsonProperty("isCompactSummary") boolean isCompactSummary,
    @JsonProperty("toolUseResult") Object toolUseResult,
    @JsonProperty("origin") MessageOrigin origin,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public UserMessage {
    }

    /**
     * Convenience constructor for simple user messages.
     */
    public UserMessage(String uuid, MessageContent message) {
        this(uuid, message, false, false, null, MessageOrigin.USER, null, Instant.now());
    }

    @Override
    public String type() {
        return "user";
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
