package com.claudecode.services.compact;

import com.claudecode.core.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * ReactiveCompact — reactive compaction based on context usage patterns.
 * Triggers when recent messages have high tool output ratio.
 */
public class ReactiveCompact {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveCompact.class);

    /** Threshold ratio of tokens used vs max before triggering compaction. */
    private static final double TOKEN_THRESHOLD = 0.8;

    /** Minimum number of messages before considering compaction. */
    private static final int MIN_MESSAGES = 10;

    /** Check if reactive compaction should trigger. */
    public boolean shouldTrigger(List<Message> messages, long currentTokens, long maxTokens) {
        if (messages == null || messages.size() < MIN_MESSAGES) {
            return false;
        }
        if (maxTokens <= 0) {
            return false;
        }

        double ratio = (double) currentTokens / maxTokens;
        if (ratio >= TOKEN_THRESHOLD) {
            LOG.info("Reactive compaction triggered: token ratio {}/{} = {}",
                    currentTokens, maxTokens, String.format("%.2f", ratio));
            return true;
        }

        return false;
    }

    /** Execute reactive compaction — keeps system messages and recent messages. */
    public List<Message> compact(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages != null ? messages : List.of();
        }

        if (messages.size() <= MIN_MESSAGES) {
            return messages;
        }

        // Keep first message (usually system) and last N messages
        int keepRecent = Math.min(MIN_MESSAGES, messages.size());
        List<Message> compacted = new ArrayList<>();

        // Always keep the first message
        compacted.add(messages.get(0));

        // Keep the most recent messages
        int startIdx = messages.size() - keepRecent;
        if (startIdx <= 0) {
            return messages;
        }

        for (int i = startIdx; i < messages.size(); i++) {
            compacted.add(messages.get(i));
        }

        LOG.info("Compacted {} messages to {}", messages.size(), compacted.size());
        return compacted;
    }
}
