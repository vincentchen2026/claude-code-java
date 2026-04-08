package com.claudecode.session;

import com.claudecode.core.message.Message;
import com.claudecode.core.message.MessageContent;
import com.claudecode.core.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TranscriptRecorder fire-and-forget async writes.
 */
class TranscriptRecorderTest {

    @TempDir
    Path tempDir;

    private SessionManager sessionManager;
    private TranscriptRecorder recorder;
    private SessionStorage storage;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager(tempDir);
        storage = new SessionStorage();
        recorder = new TranscriptRecorder(sessionManager, storage);
    }

    @Test
    void recordTranscriptWritesAsynchronously() throws InterruptedException {
        String sessionId = sessionManager.createSession();
        UserMessage msg = new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("Async test"));

        recorder.recordTranscript(sessionId, msg);

        // Give the virtual thread time to complete
        Thread.sleep(500);

        Path sessionFile = sessionManager.getSessionFile(sessionId);
        List<Message> messages = storage.readMessages(sessionFile);
        assertEquals(1, messages.size());
        assertEquals(msg.uuid(), messages.get(0).uuid());
    }

    @Test
    void recordTranscriptDoesNotThrowOnError() {
        // Use a non-existent session with a path that can't be created
        // The recorder should log the error but not throw
        assertDoesNotThrow(() -> {
            // Record to a session whose directory doesn't exist yet — this should still work
            // because appendMessage creates directories
            String sessionId = "nonexistent-" + UUID.randomUUID();
            UserMessage msg = new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("test"));
            recorder.recordTranscript(sessionId, msg);
            // Give time for async write
            Thread.sleep(500);
        });
    }

    @Test
    void multipleAsyncWritesAllPersisted() throws InterruptedException {
        String sessionId = sessionManager.createSession();
        int count = 5;

        for (int i = 0; i < count; i++) {
            UserMessage msg = new UserMessage(
                    UUID.randomUUID().toString(),
                    MessageContent.ofText("Message " + i)
            );
            recorder.recordTranscript(sessionId, msg);
        }

        // Wait for all virtual threads to complete
        Thread.sleep(2000);

        Path sessionFile = sessionManager.getSessionFile(sessionId);
        List<Message> messages = storage.readMessages(sessionFile);
        assertEquals(count, messages.size());
    }
}
