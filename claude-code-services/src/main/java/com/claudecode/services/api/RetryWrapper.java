package com.claudecode.services.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryWrapper<T> {

    private static final Logger log = LoggerFactory.getLogger(RetryWrapper.class);

    private final Supplier<T> action;
    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double multiplier;
    private final Predicate<Throwable> shouldRetry;

    private RetryWrapper(Supplier<T> action, Builder<T> builder) {
        this.action = action;
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.multiplier = builder.multiplier;
        this.shouldRetry = builder.shouldRetry;
    }

    public static <T> RetryWrapper<T> of(Supplier<T> action) {
        return builder(action).build();
    }

    public static <T> Builder<T> builder(Supplier<T> action) {
        return new Builder<>(action);
    }

    public T execute() throws RetryExhaustedException {
        int attempt = 0;
        Duration delay = initialDelay;

        while (true) {
            try {
                return action.get();
            } catch (Throwable t) {
                if (!shouldRetry.test(t) || attempt >= maxAttempts - 1) {
                    throw new RetryExhaustedException(attempt + 1, t);
                }

                attempt++;
                log.warn("Attempt {} failed, retrying in {}: {}", attempt, delay, t.getMessage());

                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RetryExhaustedException(attempt, e);
                }

                delay = Duration.ofMillis((long) Math.min(delay.toMillis() * multiplier, maxDelay.toMillis()));
            }
        }
    }

    public CompletableFuture<T> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute);
    }

    public static class Builder<T> {
        private final Supplier<T> action;
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(500);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double multiplier = 2.0;
        private Predicate<Throwable> shouldRetry = e -> true;

        private Builder(Supplier<T> action) {
            this.action = action;
        }

        public Builder<T> maxAttempts(int maxAttempts) {
            this.maxAttempts = Math.max(1, maxAttempts);
            return this;
        }

        public Builder<T> initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder<T> maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder<T> multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder<T> shouldRetry(Predicate<Throwable> shouldRetry) {
            this.shouldRetry = shouldRetry;
            return this;
        }

        public Builder<T> retryOn(Exception e) {
            this.shouldRetry = t -> t instanceof Exception;
            return this;
        }

        public RetryWrapper<T> build() {
            return new RetryWrapper<>(action, this);
        }
    }

    public static class RetryExhaustedException extends RuntimeException {
        private final int attempts;

        public RetryExhaustedException(int attempts, Throwable cause) {
            super("Retry exhausted after " + attempts + " attempt(s)", cause);
            this.attempts = attempts;
        }

        public int attempts() {
            return attempts;
        }
    }

    public static final class ApiRetryPolicy {
        private static final Duration[] DELAYS = {
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            Duration.ofSeconds(10)
        };

        public static final ApiRetryPolicy INSTANCE = new ApiRetryPolicy();

        private ApiRetryPolicy() {}

        public Duration getDelay(int attempt) {
            if (attempt < 0 || attempt >= DELAYS.length) {
                return DELAYS[DELAYS.length - 1];
            }
            return DELAYS[attempt];
        }

        public int getMaxAttempts() {
            return DELAYS.length;
        }

        public Predicate<Throwable> defaultShouldRetry() {
            return e -> !(e instanceof java.util.concurrent.TimeoutException);
        }

        public Predicate<Throwable> rateLimitShouldRetry() {
            return e -> e instanceof RateLimitException;
        }

        public <T> RetryWrapper<T> wrap(Supplier<T> action) {
            return RetryWrapper.<T>builder(action)
                .maxAttempts(getMaxAttempts())
                .initialDelay(getDelay(0))
                .maxDelay(Duration.ofSeconds(30))
                .multiplier(1.5)
                .shouldRetry(defaultShouldRetry())
                .build();
        }
    }

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }

        public RateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}