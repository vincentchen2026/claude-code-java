package com.claudecode.services.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Event recording service with privacy level checks.
 */
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsSink sink;
    private final PrivacyLevel privacyLevel;
    private final ConcurrentLinkedQueue<AnalyticsEvent> pendingEvents = new ConcurrentLinkedQueue<>();

    public AnalyticsService(AnalyticsSink sink, PrivacyLevel privacyLevel) {
        this.sink = sink;
        this.privacyLevel = privacyLevel;
    }

    /**
     * Records an analytics event if it passes the privacy check.
     */
    public boolean recordEvent(String eventName, Map<String, Object> properties) {
        if (!isAllowed(eventName)) {
            log.debug("Event {} blocked by privacy level {}", eventName, privacyLevel);
            return false;
        }

        AnalyticsEvent event = new AnalyticsEvent(eventName, properties, Instant.now());
        pendingEvents.add(event);

        if (sink.isActive()) {
            return sink.send(event);
        }
        return true;
    }

    /**
     * Flushes all pending events to the sink.
     */
    public int flush() {
        int count = 0;
        AnalyticsEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (sink.isActive()) {
                sink.send(event);
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the current privacy level.
     */
    public PrivacyLevel getPrivacyLevel() {
        return privacyLevel;
    }

    /**
     * Returns the number of pending events.
     */
    public int pendingCount() {
        return pendingEvents.size();
    }

    private boolean isAllowed(String eventName) {
        return switch (privacyLevel) {
            case DISABLED -> false;
            case MINIMAL -> eventName.startsWith("session.") || eventName.startsWith("error.");
            case STANDARD -> true;
            case VERBOSE -> true;
        };
    }

    /**
     * Privacy level for analytics collection.
     */
    public enum PrivacyLevel {
        DISABLED,
        MINIMAL,
        STANDARD,
        VERBOSE
    }

    /**
     * Analytics event record.
     */
    public record AnalyticsEvent(String name, Map<String, Object> properties, Instant timestamp) {}
}
