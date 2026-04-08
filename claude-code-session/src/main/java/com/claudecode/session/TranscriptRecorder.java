package com.claudecode.session;

import com.claudecode.core.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fire-and-forget async transcript writer.
 * <p>
 * Uses virtual threads for non-blocking writes. Errors are logged but never propagated
 * to the caller, ensuring that transcript persistence never disrupts the main conversation flow.
 */
public class TranscriptRecorder {

    private static final Logger log = LoggerFactory.getLogger(TranscriptRecorder.class);

    private final SessionManager sessionManager;
    private final SessionStorage sessionStorage;

    public TranscriptRecorder(SessionManager sessionManager, SessionStorage sessionStorage) {
        this.sessionManager = sessionManager;
        this.sessionStorage = sessionStorage;
    }

    public TranscriptRecorder(SessionManager sessionManager) {
        this(sessionManager, new SessionStorage());
    }

    /**
     * Asynchronously records a message to the session transcript.
     * <p>
     * The write is performed on a virtual thread with file locking for concurrent safety.
     * The caller does not wait for the write to complete. Errors are logged but not propagated.
     *
     * @param sessionId the session to write to
     * @param message   the message to record
     */
    public void recordTranscript(String sessionId, Message message) {
        Thread.ofVirtual().name("transcript-writer-" + sessionId)
                .start(() -> {
                    try {
                        var sessionFile = sessionManager.getSessionFile(sessionId);
                        SessionFileLock.withLock(sessionFile, () ->
                                sessionStorage.appendMessage(sessionFile, message));
                    } catch (Exception e) {
                        log.error("Failed to record transcript for session {}: {}",
                                sessionId, e.getMessage(), e);
                    }
                });
    }
}
