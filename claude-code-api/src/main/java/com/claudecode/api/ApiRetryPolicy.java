package com.claudecode.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * API request retry policy with exponential backoff.
 * Retries on 429 (rate limit), 529 (overloaded), and 5xx (server error) status codes.
 */
public class ApiRetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(ApiRetryPolicy.class);

    private final int maxRetries;
    private final Duration baseDelay;

    public ApiRetryPolicy() {
        this(5, Duration.ofSeconds(1));
    }

    public ApiRetryPolicy(int maxRetries, Duration baseDelay) {
        this.maxRetries = maxRetries;
        this.baseDelay = baseDelay;
    }

    /**
     * Executes the given action with retry logic.
     *
     * @param action  the action to execute
     * @param onRetry callback invoked before each retry attempt
     * @param <T>     the return type
     * @return the result of the action
     * @throws ApiException if all retries are exhausted or a non-retryable error occurs
     */
    public <T> T executeWithRetry(Supplier<T> action, Consumer<RetryEvent> onRetry) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (ApiException e) {
                if (!isRetryable(e) || attempt >= maxRetries) {
                    throw e;
                }
                Duration delay = calculateDelay(attempt, e);
                if (onRetry != null) {
                    onRetry.accept(new RetryEvent(attempt, maxRetries, delay, e));
                }
                log.debug("Retrying API request (attempt {}/{}), delay: {}ms, status: {}",
                        attempt + 1, maxRetries, delay.toMillis(), e.statusCode());
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiException("Retry interrupted", 0, ie);
                }
                attempt++;
            }
        }
    }

    /**
     * Convenience overload without retry callback.
     */
    public <T> T executeWithRetry(Supplier<T> action) {
        return executeWithRetry(action, null);
    }

    /**
     * Determines if an API exception is retryable.
     */
    public boolean isRetryable(ApiException e) {
        int status = e.statusCode();
        return status == 429 || status == 529 || (status >= 500 && status < 600);
    }

    /**
     * Calculates the delay before the next retry attempt.
     * Uses exponential backoff with optional Retry-After header support.
     */
    Duration calculateDelay(int attempt, ApiException e) {
        // Exponential backoff: baseDelay * 2^attempt
        long delayMs = baseDelay.toMillis() * (long) Math.pow(2, attempt);
        // Cap at 60 seconds
        delayMs = Math.min(delayMs, 60_000);
        return Duration.ofMillis(delayMs);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Duration getBaseDelay() {
        return baseDelay;
    }
}
