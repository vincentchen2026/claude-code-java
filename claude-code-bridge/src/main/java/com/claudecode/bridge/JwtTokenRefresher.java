package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Periodically refreshes JWT tokens using a ScheduledExecutorService.
 */
public class JwtTokenRefresher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenRefresher.class);
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMinutes(5);

    private final ScheduledExecutorService scheduler;
    private final Supplier<String> tokenSupplier;
    private final AtomicReference<String> currentToken = new AtomicReference<>();
    private final Duration refreshInterval;

    public JwtTokenRefresher(Supplier<String> tokenSupplier) {
        this(tokenSupplier, DEFAULT_REFRESH_INTERVAL);
    }

    public JwtTokenRefresher(Supplier<String> tokenSupplier, Duration refreshInterval) {
        this.tokenSupplier = tokenSupplier;
        this.refreshInterval = refreshInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jwt-token-refresher");
            t.setDaemon(true);
            return t;
        });
    }

    /** Starts the periodic token refresh. */
    public void start() {
        // Fetch initial token
        refreshToken();
        // Schedule periodic refresh
        scheduler.scheduleAtFixedRate(
            this::refreshToken,
            refreshInterval.toMillis(),
            refreshInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );
        log.info("JWT token refresher started (interval: {}s)", refreshInterval.toSeconds());
    }

    /** Returns the current token. */
    public String getCurrentToken() {
        return currentToken.get();
    }

    private void refreshToken() {
        try {
            String token = tokenSupplier.get();
            currentToken.set(token);
            log.debug("JWT token refreshed");
        } catch (Exception e) {
            log.warn("Failed to refresh JWT token: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
