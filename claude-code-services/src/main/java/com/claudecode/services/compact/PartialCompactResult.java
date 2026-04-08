package com.claudecode.services.compact;

import com.claudecode.core.message.Message;

import java.util.List;

/**
 * Result of a partial compaction operation.
 *
 * @param keptMessages     the filtered messages that were kept (not compacted)
 * @param compactionResult the compaction result for the compacted portion (may be null)
 * @param direction        the direction used ("from" or "up_to")
 * @param pivotIndex       the pivot index used for splitting
 */
public record PartialCompactResult(
    List<Message> keptMessages,
    CompactionResult compactionResult,
    String direction,
    int pivotIndex
) {

    /**
     * Returns true if the compacted portion was successfully summarized.
     */
    public boolean hasCompaction() {
        return compactionResult != null;
    }
}
