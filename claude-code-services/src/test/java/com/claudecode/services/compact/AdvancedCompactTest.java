package com.claudecode.services.compact;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedCompactTest {

    @Test
    void snipCompactPassesThrough() {
        var snip = new SnipCompact();
        var result = snip.snipCompact(List.of(), 1000);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertNotNull(snip.snipProjection(List.of()));
    }

    @Test
    void reactiveCompactDoesNotTriggerBelowThreshold() {
        var reactive = new ReactiveCompact();
        // Below threshold: 100/1000 = 0.1 < 0.8
        assertFalse(reactive.shouldTrigger(List.of(), 100, 1000));
    }

    @Test
    void reactiveCompactDoesNotTriggerWithFewMessages() {
        var reactive = new ReactiveCompact();
        // Even with high token ratio, too few messages
        assertFalse(reactive.shouldTrigger(List.of(), 900, 1000));
    }

    @Test
    void reactiveCompactReturnsInputForSmallList() {
        var reactive = new ReactiveCompact();
        var result = reactive.compact(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void reactiveCompactHandlesNull() {
        var reactive = new ReactiveCompact();
        assertFalse(reactive.shouldTrigger(null, 100, 1000));
        var result = reactive.compact(null);
        assertNotNull(result);
    }

    @Test
    void sessionMemoryCompactReturnsEmpty() {
        var smc = new SessionMemoryCompact();
        assertTrue(smc.extractMemories(List.of()).isEmpty());
        assertNotNull(smc.compactWithMemory(List.of()));
    }

    @Test
    void apiMicroCompactPassesThrough() {
        var amc = new ApiMicroCompact();
        assertNotNull(amc.microCompact(List.of()));
    }

    @Test
    void compactCacheConfigDefaults() {
        var config = new CompactCacheConfig();
        assertEquals(Duration.ofMinutes(5), config.getCacheTtl());
        assertEquals(100, config.getMaxCacheEntries());
        assertFalse(config.isEnabled());
        assertNull(config.getCached("key"));
        config.putCached("key", "value"); // no-op stub
    }
}
