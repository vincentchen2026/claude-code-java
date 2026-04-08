package com.claudecode.bridge;

import java.util.regex.Pattern;

/**
 * Session ID compatibility — detects session_* and cse_* format IDs.
 */
public final class SessionIdCompat {

    private SessionIdCompat() {}

    private static final Pattern SESSION_PATTERN = Pattern.compile("^session_[a-zA-Z0-9_-]+$");
    private static final Pattern CSE_PATTERN = Pattern.compile("^cse_[a-zA-Z0-9_-]+$");

    /**
     * Detects the session ID format.
     */
    public static SessionFormat detectFormat(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return SessionFormat.UNKNOWN;
        }
        if (SESSION_PATTERN.matcher(sessionId).matches()) {
            return SessionFormat.SESSION;
        }
        if (CSE_PATTERN.matcher(sessionId).matches()) {
            return SessionFormat.CSE;
        }
        return SessionFormat.UNKNOWN;
    }

    /**
     * Returns whether the session ID is in a recognized format.
     */
    public static boolean isValid(String sessionId) {
        return detectFormat(sessionId) != SessionFormat.UNKNOWN;
    }

    /**
     * Normalizes a session ID to the session_* format.
     */
    public static String normalize(String sessionId) {
        if (sessionId == null) return null;
        SessionFormat format = detectFormat(sessionId);
        return switch (format) {
            case SESSION -> sessionId;
            case CSE -> "session_" + sessionId.substring("cse_".length());
            case UNKNOWN -> sessionId;
        };
    }

    public enum SessionFormat {
        SESSION, CSE, UNKNOWN
    }
}
