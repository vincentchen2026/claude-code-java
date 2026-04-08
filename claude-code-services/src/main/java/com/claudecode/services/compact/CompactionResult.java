package com.claudecode.services.compact;

import com.claudecode.core.message.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a full compaction operation.
 */
public record CompactionResult(
    SystemMessage boundaryMarker,
    List<Message> summaryMessages,
    List<Message> attachments,
    long preCompactTokenCount
) {

    /**
     * Build the complete post-compact message list:
     * boundary marker → summary messages → attachments.
     */
    public List<Message> buildPostCompactMessages() {
        List<Message> result = new ArrayList<>();
        result.add(boundaryMarker);
        result.addAll(summaryMessages);
        result.addAll(attachments);
        return result;
    }
}
