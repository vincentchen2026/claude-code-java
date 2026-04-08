package com.claudecode.services.telemetry;

/**
 * OpenTelemetry SDK integration interface.
 * Provides trace, metric, and log recording capabilities.
 */
public interface TelemetryProvider {

    /** Records a trace span. */
    void recordSpan(String name, java.util.Map<String, String> attributes);

    /** Records a metric value. */
    void recordMetric(String name, double value, java.util.Map<String, String> tags);

    /** Records a log entry. */
    void recordLog(String level, String message);

    /** Flushes pending telemetry data. */
    void flush();

    /** Shuts down the telemetry provider. */
    void shutdown();

    /** Returns whether telemetry is enabled. */
    boolean isEnabled();

    /** No-op implementation when telemetry is disabled. */
    static TelemetryProvider noOp() {
        return new NoOpTelemetryProvider();
    }
}

class NoOpTelemetryProvider implements TelemetryProvider {
    @Override public void recordSpan(String name, java.util.Map<String, String> attributes) {}
    @Override public void recordMetric(String name, double value, java.util.Map<String, String> tags) {}
    @Override public void recordLog(String level, String message) {}
    @Override public void flush() {}
    @Override public void shutdown() {}
    @Override public boolean isEnabled() { return false; }
}
