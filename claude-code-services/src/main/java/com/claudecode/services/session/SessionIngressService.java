package com.claudecode.services.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionIngressService {

    private static final Logger log = LoggerFactory.getLogger(SessionIngressService.class);

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionTokens = new ConcurrentHashMap<>();
    private volatile String currentSessionId;

    public SessionIngressService() {
        this.currentSessionId = generateSessionId();
    }

    public String createSession(CreateSessionRequest request) {
        String sessionId = generateSessionId();
        SessionEntry entry = new SessionEntry(
            sessionId,
            request.userId(),
            request.workspaceId(),
            Instant.now(),
            SessionStatus.ACTIVE,
            request.metadata()
        );
        sessions.put(sessionId, entry);

        if (request.token() != null) {
            sessionTokens.put(sessionId, request.token());
        }

        log.info("Created session: {} for user: {}", sessionId, request.userId());
        return sessionId;
    }

    public Optional<SessionEntry> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Optional<String> getSessionToken(String sessionId) {
        return Optional.ofNullable(sessionTokens.get(sessionId));
    }

    public void updateSessionStatus(String sessionId, SessionStatus status) {
        SessionEntry existing = sessions.get(sessionId);
        if (existing != null) {
            SessionEntry updated = new SessionEntry(
                existing.sessionId(),
                existing.userId(),
                existing.workspaceId(),
                existing.createdAt(),
                status,
                existing.metadata()
            );
            sessions.put(sessionId, updated);
            log.debug("Updated session {} status to {}", sessionId, status);
        }
    }

    public void closeSession(String sessionId) {
        updateSessionStatus(sessionId, SessionStatus.CLOSED);
        log.info("Closed session: {}", sessionId);
    }

    public void attachMetadata(String sessionId, Map<String, String> metadata) {
        SessionEntry existing = sessions.get(sessionId);
        if (existing != null) {
            Map<String, String> newMetadata = new ConcurrentHashMap<>(existing.metadata());
            newMetadata.putAll(metadata);
            SessionEntry updated = new SessionEntry(
                existing.sessionId(),
                existing.userId(),
                existing.workspaceId(),
                existing.createdAt(),
                existing.status(),
                newMetadata
            );
            sessions.put(sessionId, updated);
        }
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
            .filter(e -> e.status() == SessionStatus.ACTIVE)
            .count();
    }

    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public record CreateSessionRequest(
        String userId,
        String workspaceId,
        String token,
        Map<String, String> metadata
    ) {}

    public record SessionEntry(
        String sessionId,
        String userId,
        String workspaceId,
        Instant createdAt,
        SessionStatus status,
        Map<String, String> metadata
    ) {}

    public enum SessionStatus {
        PENDING,
        ACTIVE,
        IDLE,
        CLOSED
    }
}