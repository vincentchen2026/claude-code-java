package com.claudecode.services.compact;

import com.claudecode.core.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SessionMemoryCompact — preserves key session memories during compaction (P2 stub).
 */
public class SessionMemoryCompact {

    private static final Logger LOG = LoggerFactory.getLogger(SessionMemoryCompact.class);

    /** Extract session memories before compaction. */
    public List<String> extractMemories(List<Message> messages) {
        LOG.debug("SessionMemoryCompact.extractMemories: Not yet implemented");
        return List.of();
    }

    /** Compact with memory preservation. */
    public List<Message> compactWithMemory(List<Message> messages) {
        LOG.debug("SessionMemoryCompact.compactWithMemory: Not yet implemented");
        return messages;
    }
}
