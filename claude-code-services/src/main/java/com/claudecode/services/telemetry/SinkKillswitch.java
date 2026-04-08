package com.claudecode.services.telemetry;

/**
 * Emergency shutoff for the analytics sink.
 * Can be triggered remotely or locally to immediately stop all data collection.
 */
public class SinkKillswitch {

    private final AnalyticsSink sink;

    SinkKillswitch(AnalyticsSink sink) {
        this.sink = sink;
    }

    /**
     * Triggers the killswitch, deactivating the sink.
     */
    public void trigger() {
        sink.deactivate();
    }

    /**
     * Resets the killswitch, reactivating the sink.
     */
    public void reset() {
        sink.activate();
    }

    /**
     * Returns whether the sink is currently killed.
     */
    public boolean isTriggered() {
        return !sink.isActive();
    }
}
