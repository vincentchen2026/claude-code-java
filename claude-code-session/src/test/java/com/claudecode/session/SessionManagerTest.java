package com.claudecode.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionManager: session creation, directory resolution, and listing.
 */
class SessionManagerTest {

    @TempDir
    Path tempDir;

    private SessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new SessionManager(tempDir);
    }

    @Test
    void createSessionReturnsUuidAndCreatesDirectory() {
        String sessionId = manager.createSession();

        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());
        assertTrue(Files.isDirectory(manager.getSessionDir(sessionId)));
    }

    @Test
    void getSessionDirResolvesCorrectly() {
        String id = "test-session-id";
        Path dir = manager.getSessionDir(id);
        assertEquals(tempDir.resolve("sessions").resolve(id), dir);
    }

    @Test
    void getSessionFileResolvesCorrectly() {
        String id = "test-session-id";
        Path file = manager.getSessionFile(id);
        assertEquals(tempDir.resolve("sessions").resolve(id).resolve("transcript.jsonl"), file);
    }

    @Test
    void listSessionsReturnsEmptyWhenNoSessions() {
        List<SessionInfo> sessions = manager.listSessions();
        assertTrue(sessions.isEmpty());
    }

    @Test
    void listSessionsFindsCreatedSessions() throws IOException {
        String id1 = manager.createSession();
        String id2 = manager.createSession();

        // Write some content to one session
        Path file1 = manager.getSessionFile(id1);
        Files.writeString(file1, "{\"type\":\"system\",\"uuid\":\"u1\",\"subtype\":\"info\",\"level\":\"info\",\"content\":\"test\"}\n");

        List<SessionInfo> sessions = manager.listSessions();
        assertEquals(2, sessions.size());

        // Find the session with messages
        SessionInfo withMessages = sessions.stream()
                .filter(s -> s.id().equals(id1))
                .findFirst()
                .orElseThrow();
        assertEquals(1, withMessages.messageCount());

        SessionInfo empty = sessions.stream()
                .filter(s -> s.id().equals(id2))
                .findFirst()
                .orElseThrow();
        assertEquals(0, empty.messageCount());
    }

    @Test
    void listSessionsSkipsNonDirectoryEntries() throws IOException {
        // Create a regular file in the sessions directory
        Path sessionsDir = tempDir.resolve("sessions");
        Files.createDirectories(sessionsDir);
        Files.writeString(sessionsDir.resolve("not-a-session.txt"), "hello");

        manager.createSession();

        List<SessionInfo> sessions = manager.listSessions();
        assertEquals(1, sessions.size());
    }
}
