package com.claudecode.services.memory;

import java.time.Instant;

/**
 * A single memory entry with typed category.
 */
public record MemoryEntry(
    String id,
    MemoryCategory category,
    String content,
    String source,
    Instant createdAt
) {

    public enum MemoryCategory {
        USER,
        FEEDBACK,
        PROJECT,
        REFERENCE
    }

    /**
     * Create a new memory entry with auto-generated timestamp.
     */
    public static MemoryEntry of(String id, MemoryCategory category, String content, String source) {
        return new MemoryEntry(id, category, content, source, Instant.now());
    }
}
