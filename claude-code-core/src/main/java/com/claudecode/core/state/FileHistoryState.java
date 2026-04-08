package com.claudecode.core.state;

import java.util.Map;
import java.util.Set;

/**
 * Placeholder record for file history state.
 * Tracks files that have been read/written during the session.
 *
 * @param readFiles    set of file paths that have been read
 * @param writtenFiles set of file paths that have been written
 * @param metadata     additional metadata keyed by file path
 */
public record FileHistoryState(
    Set<String> readFiles,
    Set<String> writtenFiles,
    Map<String, Object> metadata
) {
    /** Empty file history state. */
    public static final FileHistoryState EMPTY =
        new FileHistoryState(Set.of(), Set.of(), Map.of());
}
