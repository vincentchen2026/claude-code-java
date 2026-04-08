package com.claudecode.api;

import java.time.Duration;

/**
 * Event emitted before each retry attempt.
 */
public record RetryEvent(
        int attempt,
        int maxRetries,
        Duration delay,
        ApiException exception
) {}
