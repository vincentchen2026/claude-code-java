package com.claudecode.core.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Optional;

/**
 * Sealed interface for all message types in the conversation.
 * Corresponds to the TypeScript version's Message union type.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserMessage.class, name = "user"),
    @JsonSubTypes.Type(value = AssistantMessage.class, name = "assistant"),
    @JsonSubTypes.Type(value = SystemMessage.class, name = "system"),
    @JsonSubTypes.Type(value = ProgressMessage.class, name = "progress"),
    @JsonSubTypes.Type(value = AttachmentMessage.class, name = "attachment"),
    @JsonSubTypes.Type(value = HookResultMessage.class, name = "hook_result"),
    @JsonSubTypes.Type(value = ToolUseSummaryMessage.class, name = "tool_use_summary"),
    @JsonSubTypes.Type(value = TombstoneMessage.class, name = "tombstone"),
    @JsonSubTypes.Type(value = GroupedToolUseMessage.class, name = "grouped_tool_use")
})
public sealed interface Message permits
    UserMessage, AssistantMessage, SystemMessage,
    ProgressMessage, AttachmentMessage, HookResultMessage,
    ToolUseSummaryMessage, TombstoneMessage, GroupedToolUseMessage {

    String uuid();

    String type();

    Optional<String> parentUuid();

    Optional<Instant> timestamp();
}
