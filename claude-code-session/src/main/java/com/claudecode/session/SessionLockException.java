package com.claudecode.session;

/**
 * Thrown when a session file lock cannot be acquired within the timeout.
 */
public class SessionLockException extends RuntimeException {

    public SessionLockException(String message) {
        super(message);
    }

    public SessionLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
