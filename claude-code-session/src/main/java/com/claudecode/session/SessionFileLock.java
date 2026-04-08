package com.claudecode.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/**
 * File-based locking utility for concurrent session write safety.
 * Uses {@link java.nio.channels.FileLock} for exclusive access during writes.
 */
public final class SessionFileLock {

    private static final Logger log = LoggerFactory.getLogger(SessionFileLock.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(50);

    private SessionFileLock() {
        // utility class
    }

    /**
     * Executes the given action while holding an exclusive file lock on the specified file.
     * A {@code .lock} companion file is used so the actual data file is not affected.
     *
     * @param file    the data file to protect
     * @param action  the action to run under the lock
     * @throws SessionLockException if the lock cannot be acquired within the timeout
     */
    public static void withLock(Path file, Runnable action) {
        withLock(file, action, DEFAULT_TIMEOUT);
    }

    /**
     * Executes the given action while holding an exclusive file lock with a custom timeout.
     */
    public static void withLock(Path file, Runnable action, Duration timeout) {
        Path lockFile = file.resolveSibling(file.getFileName() + ".lock");
        try {
            Path parent = lockFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create lock file directory", e);
        }

        long deadline = System.nanoTime() + timeout.toNanos();

        try (FileChannel channel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            FileLock lock = tryAcquireLock(channel, deadline);
            try {
                action.run();
            } finally {
                lock.release();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("File lock I/O error for " + file, e);
        }
    }

    private static FileLock tryAcquireLock(FileChannel channel, long deadline) throws IOException {
        while (true) {
            try {
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    return lock;
                }
            } catch (OverlappingFileLockException e) {
                // Another thread in this JVM already holds the lock; retry
            }

            if (System.nanoTime() >= deadline) {
                throw new SessionLockException("Failed to acquire file lock within timeout");
            }

            try {
                Thread.sleep(RETRY_INTERVAL.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new SessionLockException("Interrupted while waiting for file lock");
            }
        }
    }
}
