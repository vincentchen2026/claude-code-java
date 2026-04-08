package com.claudecode.core.engine;

import com.claudecode.core.message.Message;

import java.util.List;

/**
 * Interface for message compaction operations used by the query engine.
 * Defined in core to avoid circular dependency with the services module.
 * Implementations live in claude-code-services (CompactService).
 */
public interface MessageCompactor {

    /**
     * Result of a microcompact operation.
     */
    record MicrocompactResult(List<Message> messages) {}

    /**
     * Result of a full compaction operation.
     */
    record CompactionResult(
        List<Message> postCompactMessages,
        long preCompactTokenCount
    ) {}

    /**
     * Truncate long tool outputs in the message list.
     *
     * @param messages the conversation messages
     * @return result with (possibly modified) message list
     */
    MicrocompactResult microcompactMessages(List<Message> messages);

    /**
     * Check whether auto-compaction should be triggered.
     *
     * @param messages    the conversation messages
     * @param model       the model name
     * @param querySource the source of the current query
     * @return true if auto-compaction should be triggered
     */
    boolean shouldAutoCompact(List<Message> messages, String model, String querySource);

    /**
     * Compact the conversation messages.
     *
     * @param messages      the conversation messages
     * @param isAutoCompact true if triggered automatically
     * @return the compaction result with post-compact messages
     */
    CompactionResult compactConversation(List<Message> messages, boolean isAutoCompact);
}
