package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main bridge loop — polls for work, sends ACKs, and maintains heartbeat.
 */
public class BridgeMain {

    private static final Logger log = LoggerFactory.getLogger(BridgeMain.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final BridgeTransport transport;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant lastHeartbeat = Instant.EPOCH;

    public BridgeMain(BridgeTransport transport) {
        this.transport = transport;
    }

    /**
     * Starts the main bridge loop. Blocks until {@link #stop()} is called.
     */
    public void run() {
        running.set(true);
        log.info("Bridge main loop started with transport {}", transport.version());

        while (running.get()) {
            try {
                pollForWork();
                maybeHeartbeat();
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Bridge loop error: {}", e.getMessage());
            }
        }
        log.info("Bridge main loop stopped");
    }

    /** Stops the main loop. */
    public void stop() {
        running.set(false);
    }

    /** Returns whether the bridge is currently running. */
    public boolean isRunning() {
        return running.get();
    }

    private void pollForWork() {
        // Stub: poll transport for incoming work items
        log.trace("Polling for work on {}", transport.endpoint());
    }

    private void maybeHeartbeat() {
        Instant now = Instant.now();
        if (Duration.between(lastHeartbeat, now).compareTo(HEARTBEAT_INTERVAL) >= 0) {
            sendHeartbeat();
            lastHeartbeat = now;
        }
    }

    private void sendHeartbeat() {
        log.trace("Sending heartbeat");
        // Stub: send heartbeat to transport endpoint
    }

    /** Sends an ACK for a processed work item. */
    public void ack(String workItemId) {
        log.debug("ACK work item: {}", workItemId);
        // Stub: send acknowledgment
    }
}
