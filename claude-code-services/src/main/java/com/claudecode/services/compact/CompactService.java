package com.claudecode.services.compact;

import com.claudecode.core.message.*;

import java.util.*;

/**
 * Context compact service — MicroCompact, AutoCompact, and ManualCompact layers.
 * <p>
 * Three-layer compaction mechanism:
 * <ol>
 *   <li>MicroCompact — truncate long tool outputs (per API call)</li>
 *   <li>AutoCompact — token threshold triggers full compaction (automatic)</li>
 *   <li>ManualCompact — user /compact command triggers compaction (manual)</li>
 * </ol>
 * Corresponds to src/services/compact/ directory.
 */
public class CompactService {

    /** Default truncation threshold in characters. */
    static final int TRUNCATION_THRESHOLD = 10_000;

    /** Maximum prompt-too-long retries before giving up. */
    static final int MAX_PTL_RETRIES = 3;

    /** Default auto-compact threshold ratio (93% of effective context window). */
    static final double AUTO_COMPACT_THRESHOLD_RATIO = 0.93;

    /** Default context window size (200K tokens for Claude models). */
    static final long DEFAULT_CONTEXT_WINDOW = 200_000;

    /** Reserved tokens for system prompt + tools schema. */
    static final long SYSTEM_PROMPT_RESERVE = 20_000;

    /**
     * Tool names whose results are eligible for truncation.
     */
    static final Set<String> COMPACTABLE_TOOLS = Set.of(
            "Read", "Bash", "Grep", "Glob", "Edit", "Write"
    );

    private final TokenEstimator tokenEstimator;
    private final CompactSummarizer summarizer;
    private boolean autoCompactEnabled;

    /**
     * Creates a CompactService with default settings (no summarizer, auto-compact enabled).
     */
    public CompactService() {
        this(TokenEstimator.getInstance(), null, true);
    }

    /**
     * Creates a CompactService with the given dependencies.
     */
    public CompactService(TokenEstimator tokenEstimator, CompactSummarizer summarizer,
                          boolean autoCompactEnabled) {
        this.tokenEstimator = tokenEstimator;
        this.summarizer = summarizer;
        this.autoCompactEnabled = autoCompactEnabled;
    }

    // ========== 1. MicroCompact (tool output truncation) ==========

    /**
     * Run microcompact on the given message list.
     * <ol>
     *   <li>Scan assistant messages for tool_use blocks whose tool name is compactable.</li>
     *   <li>For each matching tool_result in user messages, truncate TextBlock content
     *       that exceeds {@link #TRUNCATION_THRESHOLD}.</li>
     * </ol>
     *
     * @param messages the conversation messages
     * @return a {@link MicrocompactResult} with the (possibly modified) message list
     */
    public MicrocompactResult microcompactMessages(List<Message> messages) {
        Set<String> compactableToolIds = collectCompactableToolIds(messages);

        if (compactableToolIds.isEmpty()) {
            return new MicrocompactResult(messages);
        }

        List<Message> result = truncateToolResults(messages, compactableToolIds);
        return new MicrocompactResult(result);
    }

    /**
     * Walk assistant messages and collect tool_use IDs whose tool name is in
     * {@link #COMPACTABLE_TOOLS}.
     */
    Set<String> collectCompactableToolIds(List<Message> messages) {
        Set<String> ids = new LinkedHashSet<>();
        for (Message msg : messages) {
            if (msg instanceof AssistantMessage am
                    && am.message() != null
                    && am.message().content() != null) {
                for (ContentBlock block : am.message().content()) {
                    if (block instanceof ToolUseBlock tu
                            && COMPACTABLE_TOOLS.contains(tu.name())) {
                        ids.add(tu.id());
                    }
                }
            }
        }
        return ids;
    }

    // ========== 2. AutoCompact (automatic full compaction) ==========

    /**
     * Check whether auto-compaction should be triggered.
     * <p>
     * Trigger condition: token count exceeds ~93% of effective context window.
     * Recursive protection: skip if querySource is "compact" or "session_memory".
     *
     * @param messages    the conversation messages
     * @param model       the model name (used to determine context window size)
     * @param querySource the source of the current query
     * @return true if auto-compaction should be triggered
     */
    public boolean shouldAutoCompact(List<Message> messages, String model, String querySource) {
        // Recursive protection: compact itself and session_memory don't trigger
        if ("session_memory".equals(querySource) || "compact".equals(querySource)) {
            return false;
        }

        if (!autoCompactEnabled) {
            return false;
        }

        long tokenCount = tokenEstimator.estimateTokenCount(messages);
        long threshold = getAutoCompactThreshold(model);
        return tokenCount >= threshold;
    }

    /**
     * Auto-compact threshold: 93% of effective context window.
     */
    long getAutoCompactThreshold(String model) {
        long effectiveWindow = getEffectiveContextWindowSize(model);
        return (long) (effectiveWindow * AUTO_COMPACT_THRESHOLD_RATIO);
    }

    /**
     * Effective context window = model context window - system prompt reserve.
     */
    long getEffectiveContextWindowSize(String model) {
        return getModelContextWindow(model) - SYSTEM_PROMPT_RESERVE;
    }

    /**
     * Get the context window size for a model. Returns default for unknown models.
     */
    static long getModelContextWindow(String model) {
        if (model == null) {
            return DEFAULT_CONTEXT_WINDOW;
        }
        // Known model context windows
        return switch (model.toLowerCase()) {
            case "claude-sonnet-4-20250514", "claude-opus-4-20250514",
                 "claude-3-5-sonnet-20241022", "claude-3-opus-20240229" -> 200_000;
            case "claude-3-haiku-20240307" -> 200_000;
            default -> DEFAULT_CONTEXT_WINDOW;
        };
    }

    // ========== 3. Full Compaction (auto + manual shared) ==========

    /**
     * Compact the conversation by summarizing messages via the provided summarizer.
     * <p>
     * Flow:
     * <ol>
     *   <li>Validate messages are non-empty</li>
     *   <li>Estimate pre-compact token count</li>
     *   <li>Build compact prompt</li>
     *   <li>Call summarizer for summary (with prompt-too-long retry)</li>
     *   <li>Create compact_boundary marker</li>
     *   <li>Create summary message</li>
     *   <li>Generate post-compact attachments (placeholder)</li>
     *   <li>Return CompactionResult</li>
     * </ol>
     *
     * @param messages   the conversation messages to compact
     * @param compactSummarizer the summarizer to use for this compaction
     * @param isAuto     true if triggered automatically, false if manual
     * @return the compaction result
     * @throws CompactException if compaction fails
     */
    public CompactionResult compactConversation(List<Message> messages,
                                                 CompactSummarizer compactSummarizer,
                                                 boolean isAuto) {
        return compactConversation(messages, compactSummarizer, isAuto, null);
    }

    /**
     * Compact the conversation by summarizing messages via LLM.
     * <p>
     * Flow:
     * <ol>
     *   <li>Validate messages are non-empty</li>
     *   <li>Estimate pre-compact token count</li>
     *   <li>Build compact prompt</li>
     *   <li>Call LLM for summary (with prompt-too-long retry)</li>
     *   <li>Create compact_boundary marker</li>
     *   <li>Create summary message</li>
     *   <li>Generate post-compact attachments (placeholder)</li>
     *   <li>Return CompactionResult</li>
     * </ol>
     *
     * @param messages       the conversation messages to compact
     * @param isAutoCompact  true if triggered automatically, false if manual
     * @param customInstructions optional custom instructions for the compact prompt
     * @return the compaction result
     * @throws CompactException if compaction fails
     */
    public CompactionResult compactConversation(List<Message> messages,
                                                 boolean isAutoCompact,
                                                 String customInstructions) {
        if (summarizer == null) {
            throw new CompactException("No CompactSummarizer configured");
        }
        return compactConversation(messages, summarizer, isAutoCompact, customInstructions);
    }

    /**
     * Internal compaction implementation shared by all overloads.
     */
    private CompactionResult compactConversation(List<Message> messages,
                                                  CompactSummarizer compactSummarizer,
                                                  boolean isAutoCompact,
                                                  String customInstructions) {
        if (messages.isEmpty()) {
            throw new CompactException("Not enough messages to compact");
        }

        if (compactSummarizer == null) {
            throw new CompactException("No CompactSummarizer configured");
        }

        long preCompactTokenCount = tokenEstimator.estimateTokenCount(messages);

        // Build compact prompt
        String compactPrompt = buildCompactPrompt(customInstructions);

        // Call LLM for summary with prompt-too-long retry
        List<Message> messagesToSummarize = new ArrayList<>(messages);
        String summary = streamCompactSummary(messagesToSummarize, compactPrompt, compactSummarizer);

        if (summary == null || summary.isBlank()) {
            throw new CompactException("Failed to generate conversation summary");
        }

        // Create compact_boundary marker
        String compactType = isAutoCompact ? "auto" : "manual";
        SystemMessage boundaryMarker = createCompactBoundaryMarker(
                compactType, preCompactTokenCount);

        // Create summary message
        UserMessage summaryMessage = createCompactSummaryMessage(summary);

        // Generate post-compact attachments (placeholder)
        List<Message> attachments = createPostCompactAttachments();

        return new CompactionResult(
                boundaryMarker,
                List.of(summaryMessage),
                attachments,
                preCompactTokenCount
        );
    }

    /**
     * Call the LLM summarizer with prompt-too-long retry logic.
     * If the summarizer returns a response starting with "prompt is too long",
     * truncate the oldest message groups and retry up to {@link #MAX_PTL_RETRIES} times.
     *
     * @param messages      the messages to summarize (may be truncated on retry)
     * @param compactPrompt the compact prompt
     * @param compactSummarizer the summarizer to use
     * @return the summary text
     * @throws CompactException if all retries are exhausted
     */
    String streamCompactSummary(List<Message> messages, String compactPrompt,
                                CompactSummarizer compactSummarizer) {
        List<Message> current = new ArrayList<>(messages);
        int ptlAttempts = 0;

        while (true) {
            String response = compactSummarizer.summarize(current, compactPrompt);

            if (response != null && !response.startsWith("prompt is too long")) {
                return response;
            }

            // Prompt too long — truncate oldest message groups and retry
            ptlAttempts++;
            if (ptlAttempts > MAX_PTL_RETRIES) {
                throw new CompactException(
                        "Prompt too long after " + ptlAttempts + " retries");
            }
            current = truncateHeadForPTLRetry(current);
        }
    }

    /**
     * Truncate the oldest message group to reduce prompt size for retry.
     * Uses {@link MessageGrouping#groupByApiRound} to identify groups,
     * then removes the first group.
     *
     * @param messages the current messages
     * @return messages with the oldest group removed
     * @throws CompactException if there are not enough groups to truncate
     */
    List<Message> truncateHeadForPTLRetry(List<Message> messages) {
        List<List<Message>> groups = MessageGrouping.groupByApiRound(messages);

        if (groups.size() <= 1) {
            throw new CompactException(
                    "Cannot truncate further — only one message group remaining");
        }

        // Remove the first (oldest) group
        List<Message> result = new ArrayList<>();
        for (int i = 1; i < groups.size(); i++) {
            result.addAll(groups.get(i));
        }
        return result;
    }

    // ========== Compact boundary marker ==========

    /**
     * Create a compact_boundary system message.
     *
     * @param compactType         "auto" or "manual"
     * @param preCompactTokenCount token count before compaction
     * @return the boundary marker system message
     */
    static SystemMessage createCompactBoundaryMarker(String compactType,
                                                      long preCompactTokenCount) {
        String content = String.format(
                "[compact_boundary] type=%s, pre_compact_tokens=%d",
                compactType, preCompactTokenCount);
        return new SystemMessage(
                UUID.randomUUID().toString(),
                "compact_boundary",
                "info",
                content
        );
    }

    /**
     * Create a compact summary user message.
     */
    static UserMessage createCompactSummaryMessage(String summary) {
        return new UserMessage(
                UUID.randomUUID().toString(),
                MessageContent.ofText(summary),
                false,
                true, // isCompactSummary
                null,
                MessageOrigin.COMPACT_SUMMARY,
                null,
                java.time.Instant.now()
        );
    }

    // ========== Post-compact attachments ==========

    /**
     * Generate post-compact attachment messages.
     * Placeholder: returns empty list. Future implementation will re-read key files
     * and restore plan state.
     *
     * @return list of attachment messages (currently empty)
     */
    public List<Message> createPostCompactAttachments() {
        return List.of();
    }

    // ========== Compact prompt ==========

    /**
     * Build the compact prompt that instructs the LLM to summarize the conversation.
     */
    static String buildCompactPrompt(String customInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please provide a detailed summary of the conversation so far. ");
        sb.append("Include: key decisions made, files modified, tools used, ");
        sb.append("current task status, and any important context needed to continue. ");
        sb.append("Preserve all file paths, function names, and technical details.");

        if (customInstructions != null && !customInstructions.isBlank()) {
            sb.append("\n\nAdditional instructions: ").append(customInstructions);
        }

        return sb.toString();
    }

    // ========== 4. Partial Compact ==========

    /**
     * Compact only a portion of the conversation messages.
     * <p>
     * Depending on direction:
     * <ul>
     *   <li>"from" — compact messages from pivotIndex to end</li>
     *   <li>"up_to" — compact messages from start up to pivotIndex</li>
     * </ul>
     * Messages of type progress, compact_boundary, and compact_summary are
     * filtered out from the kept (non-compacted) portion.
     *
     * @param messages   the full conversation messages
     * @param pivotIndex the index to split at
     * @param direction  "from" or "up_to"
     * @param feedback   optional feedback/instructions for the summarizer
     * @return the partial compaction result
     * @throws CompactException if compaction fails
     */
    public PartialCompactResult partialCompactConversation(
            List<Message> messages, int pivotIndex, String direction, String feedback) {

        if (messages.isEmpty()) {
            throw new CompactException("Not enough messages to compact");
        }
        if (pivotIndex < 0 || pivotIndex >= messages.size()) {
            throw new CompactException("Pivot index out of bounds: " + pivotIndex);
        }

        List<Message> toCompact;
        List<Message> toKeep;

        if ("from".equals(direction)) {
            toCompact = new ArrayList<>(messages.subList(pivotIndex, messages.size()));
            toKeep = new ArrayList<>(messages.subList(0, pivotIndex));
        } else if ("up_to".equals(direction)) {
            toCompact = new ArrayList<>(messages.subList(0, pivotIndex));
            toKeep = new ArrayList<>(messages.subList(pivotIndex, messages.size()));
        } else {
            throw new CompactException("Invalid direction: " + direction + ". Must be 'from' or 'up_to'");
        }

        // Filter out progress, compact_boundary, compact_summary from kept portion
        List<Message> filteredKeep = filterKeptMessages(toKeep);

        // Compact the target portion
        CompactionResult compactionResult = null;
        if (!toCompact.isEmpty() && summarizer != null) {
            compactionResult = compactConversation(toCompact, summarizer, false, feedback);
        }

        return new PartialCompactResult(filteredKeep, compactionResult, direction, pivotIndex);
    }

    /**
     * Filter messages to remove progress, compact_boundary, and compact_summary types
     * from the kept portion of a partial compact.
     */
    List<Message> filterKeptMessages(List<Message> messages) {
        return messages.stream()
                .filter(msg -> !isFilterableMessage(msg))
                .toList();
    }

    /**
     * Check if a message should be filtered out during partial compact.
     */
    static boolean isFilterableMessage(Message msg) {
        if (msg instanceof SystemMessage sm) {
            String subtype = sm.subtype();
            return "progress".equals(subtype)
                    || "compact_boundary".equals(subtype)
                    || "compact_summary".equals(subtype);
        }
        if (msg instanceof UserMessage um) {
            return um.isCompactSummary();
        }
        return false;
    }

    // ========== Configuration ==========

    public boolean isAutoCompactEnabled() {
        return autoCompactEnabled;
    }

    public void setAutoCompactEnabled(boolean enabled) {
        this.autoCompactEnabled = enabled;
    }

    // ========== Private helpers for microcompact ==========

    private List<Message> truncateToolResults(List<Message> messages, Set<String> compactableToolIds) {
        List<Message> result = new ArrayList<>(messages.size());
        for (Message msg : messages) {
            if (msg instanceof UserMessage um && um.message() != null && um.message().blocks() != null) {
                List<ContentBlock> newBlocks = truncateBlocks(um.message().blocks(), compactableToolIds);
                if (newBlocks != um.message().blocks()) {
                    MessageContent newContent = MessageContent.ofBlocks(newBlocks);
                    result.add(new UserMessage(
                            um.uuid(), newContent, um.isMeta(), um.isCompactSummary(),
                            um.toolUseResult(), um.origin(), um.parentUuidValue(), um.timestampValue()));
                } else {
                    result.add(msg);
                }
            } else {
                result.add(msg);
            }
        }
        return result;
    }

    private List<ContentBlock> truncateBlocks(List<ContentBlock> blocks, Set<String> compactableToolIds) {
        boolean modified = false;
        List<ContentBlock> newBlocks = new ArrayList<>(blocks.size());
        for (ContentBlock block : blocks) {
            if (block instanceof ToolResultBlock tr && compactableToolIds.contains(tr.toolUseId())) {
                List<ContentBlock> truncatedContent = truncateToolResultContent(tr.content());
                if (truncatedContent != tr.content()) {
                    newBlocks.add(new ToolResultBlock(tr.toolUseId(), truncatedContent, tr.isError()));
                    modified = true;
                } else {
                    newBlocks.add(block);
                }
            } else {
                newBlocks.add(block);
            }
        }
        return modified ? newBlocks : blocks;
    }

    private List<ContentBlock> truncateToolResultContent(List<ContentBlock> content) {
        if (content == null) {
            return null;
        }
        boolean modified = false;
        List<ContentBlock> result = new ArrayList<>(content.size());
        for (ContentBlock block : content) {
            if (block instanceof TextBlock tb && tb.text() != null
                    && tb.text().length() > TRUNCATION_THRESHOLD) {
                String truncated = truncateText(tb.text());
                result.add(new TextBlock(truncated));
                modified = true;
            } else {
                result.add(block);
            }
        }
        return modified ? result : content;
    }

    /**
     * Truncate text: keep first N chars + "... [truncated, X chars total]".
     */
    static String truncateText(String text) {
        int totalLength = text.length();
        return text.substring(0, TRUNCATION_THRESHOLD)
                + "... [truncated, " + totalLength + " chars total]";
    }
}
