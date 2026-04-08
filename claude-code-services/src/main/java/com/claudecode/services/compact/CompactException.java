package com.claudecode.services.compact;

/**
 * Exception thrown when a compaction operation fails.
 */
public class CompactException extends RuntimeException {

    public CompactException(String message) {
        super(message);
    }

    public CompactException(String message, Throwable cause) {
        super(message, cause);
    }
}
