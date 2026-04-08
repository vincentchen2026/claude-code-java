package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * An assistant message in the conversation.
 */
public record AssistantMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("message") AssistantContent message,
    @JsonProperty("isApiErrorMessage") boolean isApiErrorMessage,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public AssistantMessage {
    }

    /**
     * Convenience constructor.
     */
    public AssistantMessage(String uuid, AssistantContent message) {
        this(uuid, message, false, null, Instant.now());
    }

    @Override
    public String type() {
        return "assistant";
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
