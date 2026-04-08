package com.claudecode.bridge;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Message flush gating with UUID deduplication.
 * Controls when messages are flushed and prevents duplicate processing.
 */
public class MessageFlushGate {

    private final AtomicBoolean gateOpen = new AtomicBoolean(true);
    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    /**
     * Opens the gate, allowing messages to be flushed.
     */
    public void open() {
        gateOpen.set(true);
    }

    /**
     * Closes the gate, preventing message flush.
     */
    public void close() {
        gateOpen.set(false);
    }

    /**
     * Returns whether the gate is currently open.
     */
    public boolean isOpen() {
        return gateOpen.get();
    }

    /**
     * Attempts to process a message with the given UUID.
     * Returns true if the message has not been seen before and the gate is open.
     */
    public boolean tryProcess(String messageId) {
        if (!gateOpen.get()) return false;
        return processedIds.add(messageId);
    }

    /**
     * Checks if a message ID has already been processed.
     */
    public boolean isDuplicate(String messageId) {
        return processedIds.contains(messageId);
    }

    /**
     * Generates a new unique message ID.
     */
    public static String newMessageId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the count of processed message IDs.
     */
    public int processedCount() {
        return processedIds.size();
    }

    /**
     * Clears the dedup set.
     */
    public void clearHistory() {
        processedIds.clear();
    }
}
