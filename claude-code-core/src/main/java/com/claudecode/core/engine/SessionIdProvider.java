package com.claudecode.core.engine;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task 75.3: SessionIdProvider — provides session IDs from config/global state
 * instead of generating new UUIDs on each QueryEngine construction.
 */
public interface SessionIdProvider {
    String getSessionId();

    /**
     * Default implementation that generates a new UUID once and reuses it.
     */
    static SessionIdProvider newUuid() {
        String id = UUID.randomUUID().toString();
        return () -> id;
    }

    /**
     * Provider that uses a fixed session ID.
     */
    static SessionIdProvider fixed(String sessionId) {
        return () -> sessionId;
    }

    /**
     * Provider that reads from a global/thread-local context.
     */
    static SessionIdProvider fromGlobal(AtomicReference<String> globalSessionId) {
        return () -> {
            String id = globalSessionId.get();
            return id != null ? id : UUID.randomUUID().toString();
        };
    }
}
