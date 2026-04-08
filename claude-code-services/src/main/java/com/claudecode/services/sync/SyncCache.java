package com.claudecode.services.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Simple file-based cache for synced settings.
 * Stores a single JSON blob with a timestamp for staleness checks.
 */
public class SyncCache {

    private static final Logger log = LoggerFactory.getLogger(SyncCache.class);
    private static final String CACHE_FILE = "sync-cache.json";
    private static final String TIMESTAMP_FILE = "sync-cache.timestamp";

    private final Path cacheDir;

    public SyncCache(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * Writes content to the cache.
     */
    public void write(String content) {
        ensureDir();
        try {
            Files.writeString(cacheDir.resolve(CACHE_FILE), content);
            Files.writeString(cacheDir.resolve(TIMESTAMP_FILE),
                String.valueOf(Instant.now().toEpochMilli()));
            log.debug("Sync cache written");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write sync cache", e);
        }
    }

    /**
     * Reads the cached content, if it exists.
     */
    public Optional<String> read() {
        Path file = cacheDir.resolve(CACHE_FILE);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            log.warn("Failed to read sync cache", e);
            return Optional.empty();
        }
    }

    /**
     * Returns the timestamp of the last cache write, if available.
     */
    public Optional<Instant> lastWriteTime() {
        Path file = cacheDir.resolve(TIMESTAMP_FILE);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            String ts = Files.readString(file).trim();
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(ts)));
        } catch (Exception e) {
            log.warn("Failed to read sync cache timestamp", e);
            return Optional.empty();
        }
    }

    /**
     * Checks if the cache is stale (older than maxAgeSeconds).
     */
    public boolean isStale(long maxAgeSeconds) {
        return lastWriteTime()
            .map(t -> Instant.now().getEpochSecond() - t.getEpochSecond() > maxAgeSeconds)
            .orElse(true);
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        try {
            Files.deleteIfExists(cacheDir.resolve(CACHE_FILE));
            Files.deleteIfExists(cacheDir.resolve(TIMESTAMP_FILE));
        } catch (IOException e) {
            log.warn("Failed to clear sync cache", e);
        }
    }

    /**
     * Returns true if the cache file exists.
     */
    public boolean exists() {
        return Files.isRegularFile(cacheDir.resolve(CACHE_FILE));
    }

    private void ensureDir() {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create cache directory", e);
        }
    }

    public Path getCacheDir() {
        return cacheDir;
    }
}
