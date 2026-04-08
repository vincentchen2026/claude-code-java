package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * A message carrying the result of a hook execution.
 */
public record HookResultMessage(
    @JsonProperty("uuid") String uuid,
    @JsonProperty("hookName") String hookName,
    @JsonProperty("content") String content,
    @JsonProperty("parentUuid") String parentUuidValue,
    @JsonProperty("timestamp") Instant timestampValue
) implements Message {

    @JsonCreator
    public HookResultMessage {
    }

    public HookResultMessage(String uuid, String hookName, String content) {
        this(uuid, hookName, content, null, Instant.now());
    }

    @Override
    public String type() {
        return "hook_result";
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
