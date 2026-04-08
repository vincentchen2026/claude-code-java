package com.claudecode.services.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages a scratchpad directory for cross-worker knowledge sharing.
 * Workers can create, read, and write temporary files in this directory.
 */
public class ScratchpadManager {

    private static final Logger log = LoggerFactory.getLogger(ScratchpadManager.class);

    private final Path scratchpadDir;

    public ScratchpadManager(Path scratchpadDir) {
        this.scratchpadDir = scratchpadDir;
    }

    /**
     * Ensures the scratchpad directory exists.
     */
    public Path ensureDirectory() {
        try {
            Files.createDirectories(scratchpadDir);
            return scratchpadDir;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create scratchpad directory", e);
        }
    }

    /**
     * Writes content to a file in the scratchpad.
     */
    public Path writeFile(String filename, String content) {
        ensureDirectory();
        Path file = scratchpadDir.resolve(filename);
        try {
            Files.writeString(file, content);
            log.debug("Wrote scratchpad file: {}", file);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write scratchpad file: " + filename, e);
        }
    }

    /**
     * Reads content from a file in the scratchpad.
     */
    public String readFile(String filename) {
        Path file = scratchpadDir.resolve(filename);
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read scratchpad file: " + filename, e);
        }
    }

    /**
     * Lists all files in the scratchpad directory.
     */
    public List<String> listFiles() {
        if (!Files.isDirectory(scratchpadDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(scratchpadDir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list scratchpad files", e);
        }
    }

    /**
     * Checks if a file exists in the scratchpad.
     */
    public boolean fileExists(String filename) {
        return Files.isRegularFile(scratchpadDir.resolve(filename));
    }

    /**
     * Deletes a file from the scratchpad.
     */
    public boolean deleteFile(String filename) {
        try {
            return Files.deleteIfExists(scratchpadDir.resolve(filename));
        } catch (IOException e) {
            log.warn("Failed to delete scratchpad file: {}", filename, e);
            return false;
        }
    }

    /**
     * Returns the scratchpad directory path.
     */
    public Path getDirectory() {
        return scratchpadDir;
    }
}
