package com.claudecode.core.message;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface for all SDK output message types that QueryEngine yields.
 * These are the messages consumed by the UI/caller layer.
 */
public sealed interface SDKMessage permits
    SDKMessage.Assistant, SDKMessage.User, SDKMessage.System,
    SDKMessage.Progress, SDKMessage.StreamEvent,
    SDKMessage.Attachment, SDKMessage.Tombstone, SDKMessage.CompactBoundary,
    SDKMessage.ToolUseSummary, SDKMessage.ApiRetry, SDKMessage.StreamRequestStart,
    SDKMessage.Result, SDKMessage.Error, SDKMessage.Sentinel {

    /** Sentinel constant for iterator termination. */
    SDKMessage SENTINEL = new Sentinel();

    /**
     * An assistant message yielded from the query engine.
     */
    record Assistant(AssistantMessage message, Usage usage) implements SDKMessage {}

    /**
     * A user message yielded from the query engine.
     */
    record User(UserMessage message) implements SDKMessage {}

    /**
     * A system message yielded from the query engine.
     */
    record System(SystemMessage message) implements SDKMessage {}

    /**
     * A progress message yielded from the query engine.
     */
    record Progress(ProgressMessage message) implements SDKMessage {}

    /**
     * A raw stream event from the API (content deltas, etc.).
     */
    record StreamEvent(String eventType, Object data) implements SDKMessage {}

    /**
     * An attachment message (structured_output, max_turns_reached, queued_command).
     */
    record Attachment(String attachmentType, String content, String parentUuid) implements SDKMessage {}

    /**
     * A tombstone message (deleted/replaced message marker).
     */
    record Tombstone(String replacedUuid) implements SDKMessage {}

    /**
     * A compact boundary message marking the boundary of a compaction operation.
     */
    record CompactBoundary(List<String> compactedMessageUuids, Usage preCompactUsage) implements SDKMessage {}

    /**
     * A tool use summary message.
     */
    record ToolUseSummary(String toolName, String toolUseId, String summary) implements SDKMessage {}

    /**
     * An API retry system message.
     */
    record ApiRetry(String reason, int retryCount) implements SDKMessage {}

    /**
     * A stream request start marker.
     */
    record StreamRequestStart(String model, int messageCount) implements SDKMessage {}

    /**
     * The final result of a query engine run.
     */
    record Result(
        String resultType,
        List<Message> messages,
        Usage totalUsage,
        String sessionId,
        double totalCost,
        List<PermissionDenial> permissionDenials,
        String fastModeState,
        boolean structuredOutputApplied
    ) implements SDKMessage {

        /** Result types */
        public static final String SUCCESS = "success";
        public static final String ERROR_DURING_EXECUTION = "error_during_execution";
        public static final String ERROR_MAX_TURNS = "error_max_turns";
        public static final String ERROR_MAX_BUDGET = "error_max_budget_usd";
        public static final String ERROR_MAX_STRUCTURED_OUTPUT_RETRIES = "error_max_structured_output_retries";

        /** Convenience constructor for backward compatibility. */
        public Result(String resultType, List<Message> messages, Usage totalUsage, String sessionId) {
            this(resultType, messages, totalUsage, sessionId, 0.0, List.of(), null, false);
        }
    }

    /**
     * Structured permission denial record (replaces List<String>).
     */
    record PermissionDenial(
        String toolName,
        String toolUseId,
        Map<String, Object> toolInput
    ) {}

    /**
     * An error that occurred during the query engine run.
     */
    record Error(Exception exception) implements SDKMessage {}

    /**
     * Sentinel record for iterator termination — not a real message.
     */
    record Sentinel() implements SDKMessage {}

    /**
     * Factory method for creating an error SDKMessage.
     */
    static SDKMessage error(Exception e) {
        return new Error(e);
    }
}
