package com.claudecode.bridge;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session deduplication using ConcurrentHashMap.
 * Prevents duplicate session processing in multi-peer scenarios.
 */
public class PeerSessionDedup {

    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    /**
     * Attempts to claim a session. Returns true if this is the first claim.
     */
    public boolean tryClaimSession(String sessionId) {
        return activeSessions.add(sessionId);
    }

    /**
     * Releases a previously claimed session.
     */
    public boolean releaseSession(String sessionId) {
        return activeSessions.remove(sessionId);
    }

    /**
     * Returns whether a session is currently claimed.
     */
    public boolean isSessionActive(String sessionId) {
        return activeSessions.contains(sessionId);
    }

    /**
     * Returns the number of active sessions.
     */
    public int activeCount() {
        return activeSessions.size();
    }

    /**
     * Clears all active sessions.
     */
    public void clear() {
        activeSessions.clear();
    }
}
