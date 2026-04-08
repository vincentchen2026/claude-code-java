package com.claudecode.session;

import com.claudecode.core.message.MessageContent;
import com.claudecode.core.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionFileLock concurrent safety.
 */
class SessionFileLockTest {

    @TempDir
    Path tempDir;

    @Test
    void concurrentWritesWithFileLock() throws InterruptedException {
        Path sessionFile = tempDir.resolve("concurrent.jsonl");
        SessionStorage storage = new SessionStorage();
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    UserMessage msg = new UserMessage(
                            UUID.randomUUID().toString(),
                            MessageContent.ofText("Message " + idx)
                    );
                    SessionFileLock.withLock(sessionFile, () ->
                            storage.appendMessage(sessionFile, msg));
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Threads did not complete in time");

        assertEquals(0, errorCount.get(), "Some writes failed");

        // Verify all messages were written
        var messages = storage.readMessages(sessionFile);
        assertEquals(threadCount, messages.size(), "Expected " + threadCount + " messages");
    }

    @Test
    void lockIsReentrantAcrossSequentialCalls() {
        Path file = tempDir.resolve("reentrant.jsonl");
        List<String> order = new CopyOnWriteArrayList<>();

        SessionFileLock.withLock(file, () -> order.add("first"));
        SessionFileLock.withLock(file, () -> order.add("second"));

        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void lockTimeoutThrowsException() {
        Path file = tempDir.resolve("timeout.jsonl");
        CountDownLatch holdingLock = new CountDownLatch(1);
        CountDownLatch tryAcquire = new CountDownLatch(1);

        // Hold the lock in a background thread
        Thread holder = Thread.ofVirtual().start(() -> {
            SessionFileLock.withLock(file, () -> {
                holdingLock.countDown();
                try {
                    // Hold lock for longer than the timeout
                    tryAcquire.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        try {
            assertTrue(holdingLock.await(5, TimeUnit.SECONDS));
            // Try to acquire with a very short timeout
            assertThrows(SessionLockException.class, () ->
                    SessionFileLock.withLock(file, () -> {}, Duration.ofMillis(200)));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        } finally {
            tryAcquire.countDown();
        }
    }
}
