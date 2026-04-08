package com.claudecode.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages session lifecycle: creation, directory resolution, and listing.
 * <p>
 * Sessions are stored under {@code ~/.claude/sessions/{id}/} with a JSONL transcript file.
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final String SESSIONS_DIR_NAME = "sessions";
    private static final String TRANSCRIPT_FILE_NAME = "transcript.jsonl";

    private final Path baseDir;

    /**
     * Creates a SessionManager using the default base directory ({@code ~/.claude}).
     */
    public SessionManager() {
        this(Path.of(System.getProperty("user.home"), ".claude"));
    }

    /**
     * Creates a SessionManager with a custom base directory (useful for testing).
     */
    public SessionManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Generates a new session ID (UUID) and creates the session directory.
     *
     * @return the new session ID
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        Path sessionDir = getSessionDir(sessionId);
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create session directory: " + sessionDir, e);
        }
        return sessionId;
    }

    /**
     * Resolves the session directory for the given session ID.
     */
    public Path getSessionDir(String sessionId) {
        return baseDir.resolve(SESSIONS_DIR_NAME).resolve(sessionId);
    }

    /**
     * Resolves the session JSONL transcript file for the given session ID.
     */
    public Path getSessionFile(String sessionId) {
        return getSessionDir(sessionId).resolve(TRANSCRIPT_FILE_NAME);
    }

    /**
     * Lists all sessions with metadata.
     * Scans the sessions directory for subdirectories and reads basic metadata.
     *
     * @return list of session info records; empty list if no sessions exist
     */
    public List<SessionInfo> listSessions() {
        Path sessionsDir = baseDir.resolve(SESSIONS_DIR_NAME);
        if (!Files.isDirectory(sessionsDir)) {
            return List.of();
        }

        List<SessionInfo> sessions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsDir)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    continue;
                }
                try {
                    SessionInfo info = buildSessionInfo(entry);
                    sessions.add(info);
                } catch (Exception e) {
                    log.warn("Skipping invalid session directory {}: {}", entry.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list sessions in " + sessionsDir, e);
        }
        return sessions;
    }

    private SessionInfo buildSessionInfo(Path sessionDir) throws IOException {
        String id = sessionDir.getFileName().toString();
        BasicFileAttributes attrs = Files.readAttributes(sessionDir, BasicFileAttributes.class);
        Instant createdAt = attrs.creationTime().toInstant();

        Path transcriptFile = sessionDir.resolve(TRANSCRIPT_FILE_NAME);
        int messageCount = 0;
        if (Files.exists(transcriptFile)) {
            messageCount = (int) Files.lines(transcriptFile)
                    .filter(line -> !line.trim().isEmpty())
                    .count();
        }

        // lastModel is not easily extractable without parsing all messages;
        // we leave it null for the listing (callers can load full session if needed)
        return new SessionInfo(id, createdAt, messageCount, null);
    }
}
