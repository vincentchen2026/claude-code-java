package com.claudecode.services.telemetry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelemetrySystemTest {

    // --- AnalyticsSink + SinkKillswitch ---

    @Test
    void sinkIsActiveByDefault() {
        var sink = new AnalyticsSink();
        assertTrue(sink.isActive());
    }

    @Test
    void killswitchDeactivatesSink() {
        var sink = new AnalyticsSink();
        var killswitch = sink.getKillswitch();

        assertFalse(killswitch.isTriggered());
        killswitch.trigger();
        assertTrue(killswitch.isTriggered());
        assertFalse(sink.isActive());

        killswitch.reset();
        assertFalse(killswitch.isTriggered());
        assertTrue(sink.isActive());
    }

    @Test
    void sinkRejectsEventsWhenInactive() {
        var sink = new AnalyticsSink();
        sink.deactivate();

        var event = new AnalyticsService.AnalyticsEvent("test", Map.of(), java.time.Instant.now());
        assertFalse(sink.send(event));
    }

    // --- AnalyticsService ---

    @Test
    void analyticsServiceRecordsEvents() {
        var sink = new AnalyticsSink();
        var service = new AnalyticsService(sink, AnalyticsService.PrivacyLevel.STANDARD);

        assertTrue(service.recordEvent("tool.used", Map.of("tool", "Bash")));
    }

    @Test
    void analyticsServiceRespectsPrivacyDisabled() {
        var sink = new AnalyticsSink();
        var service = new AnalyticsService(sink, AnalyticsService.PrivacyLevel.DISABLED);

        assertFalse(service.recordEvent("tool.used", Map.of()));
    }

    @Test
    void analyticsServiceMinimalPrivacy() {
        var sink = new AnalyticsSink();
        var service = new AnalyticsService(sink, AnalyticsService.PrivacyLevel.MINIMAL);

        assertTrue(service.recordEvent("session.start", Map.of()));
        assertTrue(service.recordEvent("error.api", Map.of()));
        assertFalse(service.recordEvent("tool.used", Map.of()));
    }

    // --- DiagnosticLog ---

    @Test
    void diagnosticLogAddsAndRetrieves() {
        var log = new DiagnosticLog(10);
        log.add("INFO", "Test message");
        assertEquals(1, log.size());

        var entries = log.getEntries();
        assertEquals(1, entries.size());
        assertEquals("INFO", entries.get(0).level());
        assertEquals("Test message", entries.get(0).message());
    }

    @Test
    void diagnosticLogRingBufferOverflow() {
        var log = new DiagnosticLog(3);
        log.add("INFO", "msg1");
        log.add("INFO", "msg2");
        log.add("INFO", "msg3");
        log.add("INFO", "msg4"); // should evict msg1

        assertEquals(3, log.size());
        var entries = log.getEntries();
        assertEquals("msg2", entries.get(0).message());
        assertEquals("msg3", entries.get(1).message());
        assertEquals("msg4", entries.get(2).message());
    }

    @Test
    void diagnosticLogClear() {
        var log = new DiagnosticLog(10);
        log.add("INFO", "msg1");
        log.add("INFO", "msg2");
        log.clear();
        assertEquals(0, log.size());
    }

    @Test
    void diagnosticLogCapacity() {
        var log = new DiagnosticLog(100);
        assertEquals(100, log.capacity());
    }

    @Test
    void diagnosticLogInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DiagnosticLog(0));
        assertThrows(IllegalArgumentException.class, () -> new DiagnosticLog(-1));
    }

    // --- TelemetryProvider ---

    @Test
    void noOpTelemetryProviderIsDisabled() {
        var provider = TelemetryProvider.noOp();
        assertFalse(provider.isEnabled());
        // Should not throw
        provider.recordSpan("test", Map.of());
        provider.recordMetric("test", 1.0, Map.of());
        provider.recordLog("INFO", "test");
        provider.flush();
        provider.shutdown();
    }

    // --- GrowthBookClient ---

    @Test
    void growthBookDisabledReturnsFalse() {
        var client = new GrowthBookClient(false);
        assertFalse(client.isFeatureEnabled("feature-x"));
        assertTrue(client.getFeatureValue("feature-x").isEmpty());
    }

    @Test
    void growthBookOverrides() {
        var client = new GrowthBookClient(true);
        client.setOverride("feature-x", true);
        assertTrue(client.isFeatureEnabled("feature-x"));

        client.setOverride("feature-y", "value-y");
        assertEquals("value-y", client.getFeatureValue("feature-y").orElse(""));

        client.clearOverrides();
        assertFalse(client.isFeatureEnabled("feature-x"));
    }

    @Test
    void growthBookEnableDisable() {
        var client = new GrowthBookClient(true);
        assertTrue(client.isEnabled());
        client.setEnabled(false);
        assertFalse(client.isEnabled());
    }
}
