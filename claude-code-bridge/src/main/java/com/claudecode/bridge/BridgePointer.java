package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Crash recovery persistent pointer.
 * Stores the last processed message offset to a file so the bridge
 * can resume after a crash without reprocessing.
 */
public class BridgePointer {

    private static final Logger log = LoggerFactory.getLogger(BridgePointer.class);
    private static final String POINTER_FILE = ".bridge-pointer";

    private final Path pointerFile;
    private volatile long offset;

    public BridgePointer(Path directory) {
        this.pointerFile = directory.resolve(POINTER_FILE);
        this.offset = load().orElse(0L);
    }

    /** Returns the current offset. */
    public long getOffset() {
        return offset;
    }

    /** Advances the pointer to the given offset and persists it. */
    public void advance(long newOffset) {
        if (newOffset < offset) {
            throw new IllegalArgumentException("Cannot move pointer backwards: " + newOffset + " < " + offset);
        }
        this.offset = newOffset;
        persist();
    }

    /** Resets the pointer to zero. */
    public void reset() {
        this.offset = 0;
        persist();
    }

    private void persist() {
        try {
            Files.createDirectories(pointerFile.getParent());
            Files.writeString(pointerFile, Long.toString(offset));
        } catch (IOException e) {
            log.warn("Failed to persist bridge pointer: {}", e.getMessage());
        }
    }

    private Optional<Long> load() {
        try {
            if (Files.exists(pointerFile)) {
                String content = Files.readString(pointerFile).trim();
                return Optional.of(Long.parseLong(content));
            }
        } catch (IOException | NumberFormatException e) {
            log.warn("Failed to load bridge pointer: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
