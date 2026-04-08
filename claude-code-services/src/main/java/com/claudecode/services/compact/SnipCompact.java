package com.claudecode.services.compact;

import com.claudecode.core.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SnipCompact — HISTORY_SNIP feature gate compact strategy (P2 stub).
 * Replaces old messages with a "[snipped]" marker to reduce context size.
 */
public class SnipCompact {

    private static final Logger LOG = LoggerFactory.getLogger(SnipCompact.class);

    /** Apply snip compaction to messages. */
    public List<Message> snipCompact(List<Message> messages, int targetTokens) {
        LOG.debug("SnipCompact: Not yet implemented (target: {} tokens)", targetTokens);
        return messages; // pass-through stub
    }

    /** Project snipped content for display. */
    public String snipProjection(List<Message> messages) {
        return "[SnipProjection: Not yet implemented]";
    }
}
