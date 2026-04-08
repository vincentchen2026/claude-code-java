package com.claudecode.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiRetryPolicy behavior.
 */
class ApiRetryPolicyTest {

    @Test
    void successfulCallReturnsImmediately() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));
        String result = policy.executeWithRetry(() -> "success");
        assertEquals("success", result);
    }

    @Test
    void retriesOn429() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));
        AtomicInteger attempts = new AtomicInteger(0);

        String result = policy.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ApiException("Rate limited", 429);
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void retriesOn529() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));
        AtomicInteger attempts = new AtomicInteger(0);

        String result = policy.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new ApiException("Overloaded", 529);
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void retriesOn500() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));
        AtomicInteger attempts = new AtomicInteger(0);

        String result = policy.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new ApiException("Server error", 500);
            }
            return "recovered";
        });

        assertEquals("recovered", result);
    }

    @Test
    void doesNotRetryOn400() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));

        ApiException thrown = assertThrows(ApiException.class, () ->
                policy.executeWithRetry(() -> {
                    throw new ApiException("Bad request", 400);
                }));

        assertEquals(400, thrown.statusCode());
    }

    @Test
    void doesNotRetryOn401() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));

        ApiException thrown = assertThrows(ApiException.class, () ->
                policy.executeWithRetry(() -> {
                    throw new ApiException("Unauthorized", 401);
                }));

        assertEquals(401, thrown.statusCode());
    }

    @Test
    void exhaustsRetriesAndThrows() {
        ApiRetryPolicy policy = new ApiRetryPolicy(2, Duration.ofMillis(10));

        ApiException thrown = assertThrows(ApiException.class, () ->
                policy.executeWithRetry(() -> {
                    throw new ApiException("Always fails", 500);
                }));

        assertEquals(500, thrown.statusCode());
    }

    @Test
    void callsRetryCallback() {
        ApiRetryPolicy policy = new ApiRetryPolicy(3, Duration.ofMillis(10));
        List<RetryEvent> events = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger(0);

        policy.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ApiException("Rate limited", 429);
            }
            return "done";
        }, events::add);

        assertEquals(2, events.size());
        assertEquals(0, events.get(0).attempt());
        assertEquals(1, events.get(1).attempt());
        assertEquals(3, events.get(0).maxRetries());
    }

    @Test
    void exponentialBackoffDelayCalculation() {
        ApiRetryPolicy policy = new ApiRetryPolicy(5, Duration.ofSeconds(1));

        Duration delay0 = policy.calculateDelay(0, new ApiException("test", 429));
        Duration delay1 = policy.calculateDelay(1, new ApiException("test", 429));
        Duration delay2 = policy.calculateDelay(2, new ApiException("test", 429));
        Duration delay3 = policy.calculateDelay(3, new ApiException("test", 429));

        assertEquals(1000, delay0.toMillis());
        assertEquals(2000, delay1.toMillis());
        assertEquals(4000, delay2.toMillis());
        assertEquals(8000, delay3.toMillis());
    }

    @Test
    void delayIsCappedAt60Seconds() {
        ApiRetryPolicy policy = new ApiRetryPolicy(10, Duration.ofSeconds(1));

        Duration delay = policy.calculateDelay(10, new ApiException("test", 429));
        assertTrue(delay.toMillis() <= 60_000, "Delay should be capped at 60 seconds");
    }

    @Test
    void isRetryableClassifiesCorrectly() {
        ApiRetryPolicy policy = new ApiRetryPolicy();

        assertTrue(policy.isRetryable(new ApiException("", 429)));
        assertTrue(policy.isRetryable(new ApiException("", 529)));
        assertTrue(policy.isRetryable(new ApiException("", 500)));
        assertTrue(policy.isRetryable(new ApiException("", 502)));
        assertTrue(policy.isRetryable(new ApiException("", 503)));
        assertFalse(policy.isRetryable(new ApiException("", 400)));
        assertFalse(policy.isRetryable(new ApiException("", 401)));
        assertFalse(policy.isRetryable(new ApiException("", 403)));
        assertFalse(policy.isRetryable(new ApiException("", 404)));
    }
}
