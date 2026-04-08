package com.claudecode.core.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task 75.2: FileStateCache — tracks file content and metadata for change detection.
 * Replaces simple Map<String, String> with a structured cache that includes
 * content, mtime, and read timestamps for read-before-write validation.
 */
public class FileStateCache {

    private final Map<String, FileState> cache = new ConcurrentHashMap<>();

    /**
     * File state record with content and metadata.
     */
    public record FileState(
        String content,
        String contentHash,
        FileTime lastModified,
        Instant readTime,
        long size
    ) {}

    /**
     * Get cached file state for a path.
     */
    public FileState get(String absolutePath) {
        return cache.get(absolutePath);
    }

    /**
     * Put file state into cache.
     */
    public void put(String absolutePath, String content) {
        try {
            Path path = Path.of(absolutePath);
            FileTime mtime = Files.exists(path) ? Files.getLastModifiedTime(path) : FileTime.fromMillis(0);
            long size = Files.exists(path) ? Files.size(path) : 0;
            String hash = computeHash(content);
            cache.put(absolutePath, new FileState(content, hash, mtime, Instant.now(), size));
        } catch (IOException e) {
            cache.put(absolutePath, new FileState(content, computeHash(content),
                FileTime.fromMillis(0), Instant.now(), content.length()));
        }
    }

    /**
     * Check if a file has been modified since it was last read.
     */
    public boolean isStale(String absolutePath) {
        FileState state = cache.get(absolutePath);
        if (state == null) return true;

        try {
            Path path = Path.of(absolutePath);
            if (!Files.exists(path)) return true;
            FileTime currentMtime = Files.getLastModifiedTime(path);
            return !currentMtime.equals(state.lastModified());
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Check if a file was read recently (within threshold).
     */
    public boolean wasReadRecently(String absolutePath, long thresholdMs) {
        FileState state = cache.get(absolutePath);
        if (state == null) return false;
        return (System.currentTimeMillis() - state.readTime().toEpochMilli()) < thresholdMs;
    }

    /**
     * Get the cached content for a path.
     */
    public String getContent(String absolutePath) {
        FileState state = cache.get(absolutePath);
        return state != null ? state.content() : null;
    }

    /**
     * Remove a path from the cache.
     */
    public void remove(String absolutePath) {
        cache.remove(absolutePath);
    }

    /**
     * Clear the entire cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get all cached paths.
     */
    public Set<String> getCachedPaths() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * Get cache size.
     */
    public int size() {
        return cache.size();
    }

    private String computeHash(String content) {
        if (content == null) return "";
        return String.valueOf(content.hashCode());
    }
}
