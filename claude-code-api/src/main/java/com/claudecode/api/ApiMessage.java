package com.claudecode.api;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.Usage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response message from the Anthropic Messages API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMessage(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("role") String role,
        @JsonProperty("content") List<ContentBlock> content,
        @JsonProperty("model") String model,
        @JsonProperty("stop_reason") String stopReason,
        @JsonProperty("stop_sequence") String stopSequence,
        @JsonProperty("usage") Usage usage
) {

    @JsonCreator
    public ApiMessage {
    }

    /**
     * Creates a stub ApiMessage for testing/placeholder responses.
     */
    public static ApiMessage stub(String model, String text) {
        return new ApiMessage(
            "msg_stub_" + System.nanoTime(),
            "message",
            "assistant",
            List.of(new TextBlock(text)),
            model,
            "end_turn",
            null,
            Usage.EMPTY
        );
    }

    /**
     * Builder for ApiMessage.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String type = "message";
        private String role = "assistant";
        private List<ContentBlock> content = List.of();
        private String model;
        private String stopReason;
        private String stopSequence;
        private Usage usage;

        public Builder id(String id) { this.id = id; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder content(List<ContentBlock> content) { this.content = content; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder stopReason(String stopReason) { this.stopReason = stopReason; return this; }
        public Builder stopSequence(String stopSequence) { this.stopSequence = stopSequence; return this; }
        public Builder usage(Usage usage) { this.usage = usage; return this; }

        public ApiMessage build() {
            return new ApiMessage(id, type, role, content, model, stopReason, stopSequence, usage);
        }
    }
}
