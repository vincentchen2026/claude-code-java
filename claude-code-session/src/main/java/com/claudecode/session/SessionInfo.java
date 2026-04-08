package com.claudecode.session;

import java.time.Instant;

/**
 * Metadata about a stored session.
 *
 * @param id           the session UUID
 * @param createdAt    when the session was created (from directory creation time)
 * @param messageCount number of messages in the session JSONL file
 * @param lastModel    the model used in the last assistant message, or null
 */
public record SessionInfo(
    String id,
    Instant createdAt,
    int messageCount,
    String lastModel
) {}
