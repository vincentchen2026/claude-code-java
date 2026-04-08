package com.claudecode.services.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Data sink for analytics events with emergency shutoff (killswitch).
 */
public class AnalyticsSink {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsSink.class);

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final SinkKillswitch killswitch;

    public AnalyticsSink() {
        this.killswitch = new SinkKillswitch(this);
    }

    /**
     * Sends an event to the analytics backend.
     * Returns false if the sink is inactive.
     */
    public boolean send(AnalyticsService.AnalyticsEvent event) {
        if (!active.get()) {
            return false;
        }
        // Stub: actual HTTP POST to analytics endpoint
        log.debug("Analytics event sent: {}", event.name());
        return true;
    }

    /**
     * Returns whether the sink is currently active.
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * Deactivates the sink (emergency shutoff).
     */
    public void deactivate() {
        active.set(false);
        log.warn("Analytics sink deactivated");
    }

    /**
     * Reactivates the sink.
     */
    public void activate() {
        active.set(true);
        log.info("Analytics sink activated");
    }

    /**
     * Returns the killswitch for this sink.
     */
    public SinkKillswitch getKillswitch() {
        return killswitch;
    }
}
