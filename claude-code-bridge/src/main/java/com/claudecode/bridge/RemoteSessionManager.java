package com.claudecode.bridge;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteSessionManager {

    private final Map<String, RemoteSession> sessions;
    private final ConcurrentLinkedQueue<SessionEvent> eventQueue;
    private final AtomicInteger sessionCounter;

    public RemoteSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.sessionCounter = new AtomicInteger(0);
    }

    public RemoteSession createSession(SessionConfig config) {
        String sessionId = generateSessionId(config.remoteId());
        RemoteSession session = new RemoteSession(
            sessionId,
            config.remoteId(),
            config.remoteUrl(),
            Instant.now(),
            SessionStatus.CONNECTING,
            config.capabilities()
        );
        
        sessions.put(sessionId, session);
        enqueueEvent(new SessionEvent(sessionId, SessionEventType.CREATED, Instant.now()));
        
        return session;
    }

    public RemoteSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void updateSessionStatus(String sessionId, SessionStatus status) {
        RemoteSession session = sessions.get(sessionId);
        if (session != null) {
            RemoteSession updated = new RemoteSession(
                session.id(),
                session.remoteId(),
                session.remoteUrl(),
                session.createdAt(),
                status,
                session.capabilities()
            );
            sessions.put(sessionId, updated);
            enqueueEvent(new SessionEvent(sessionId, SessionEventType.STATUS_CHANGED, Instant.now()));
        }
    }

    public void closeSession(String sessionId) {
        RemoteSession session = sessions.get(sessionId);
        if (session != null) {
            updateSessionStatus(sessionId, SessionStatus.CLOSED);
            enqueueEvent(new SessionEvent(sessionId, SessionEventType.CLOSED, Instant.now()));
        }
    }

    public boolean hasActiveSession(String remoteId) {
        return sessions.values().stream()
            .anyMatch(s -> s.remoteId().equals(remoteId) && 
                          (s.status() == SessionStatus.CONNECTED || s.status() == SessionStatus.CONNECTING));
    }

    public RemoteSession getActiveSession(String remoteId) {
        return sessions.values().stream()
            .filter(s -> s.remoteId().equals(remoteId) && 
                        (s.status() == SessionStatus.CONNECTED || s.status() == SessionStatus.CONNECTING))
            .findFirst()
            .orElse(null);
    }

    public java.util.List<RemoteSession> getAllSessions() {
        return new java.util.ArrayList<>(sessions.values());
    }

    public java.util.List<RemoteSession> getSessionsByStatus(SessionStatus status) {
        return sessions.values().stream()
            .filter(s -> s.status() == status)
            .toList();
    }

    public SessionEvent pollEvent() {
        return eventQueue.poll();
    }

    public int getEventQueueSize() {
        return eventQueue.size();
    }

    private void enqueueEvent(SessionEvent event) {
        eventQueue.offer(event);
    }

    private String generateSessionId(String remoteId) {
        int count = sessionCounter.incrementAndGet();
        return remoteId + "-" + count + "-" + System.currentTimeMillis();
    }

    public enum SessionStatus {
        CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, CLOSED, ERROR
    }

    public enum SessionEventType {
        CREATED, STATUS_CHANGED, CLOSED, MESSAGE_SENT, MESSAGE_RECEIVED
    }

    public record SessionConfig(
        String remoteId,
        String remoteUrl,
        String authToken,
        java.util.List<String> capabilities
    ) {}

    public record RemoteSession(
        String id,
        String remoteId,
        String remoteUrl,
        Instant createdAt,
        SessionStatus status,
        java.util.List<String> capabilities
    ) {}

    public record SessionEvent(
        String sessionId,
        SessionEventType type,
        Instant timestamp
    ) {}
}