package com.claudecode.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

/**
 * File utility methods for Claude Code.
 */
public final class FileUtils {

    private FileUtils() {
        // utility class
    }

    /**
     * Reads a file as a UTF-8 string. Returns empty Optional if the file does not exist.
     */
    public static Optional<String> readFileIfExists(Path path) {
        if (!Files.exists(path)) return Optional.empty();
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Reads a file as a UTF-8 string. Throws if the file does not exist.
     */
    public static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Writes a UTF-8 string to a file, creating parent directories as needed.
     */
    public static void writeFile(Path path, String content) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write file: " + path, e);
        }
    }

    /**
     * Appends a line to a file (JSONL-style append).
     */
    public static void appendLine(Path path, String line) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to append to file: " + path, e);
        }
    }

    /**
     * Reads all lines from a file. Returns empty list if the file does not exist.
     */
    public static List<String> readLines(Path path) {
        if (!Files.exists(path)) return List.of();
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read lines: " + path, e);
        }
    }

    /**
     * Checks if a file appears to be binary by scanning for null bytes in the first 8KB.
     */
    public static boolean isBinaryFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            int limit = Math.min(bytes.length, 8192);
            for (int i = 0; i < limit; i++) {
                if (bytes[i] == 0) return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
