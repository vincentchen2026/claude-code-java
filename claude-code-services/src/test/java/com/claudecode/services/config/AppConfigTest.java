package com.claudecode.services.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void defaultConfigHasSensibleDefaults() {
        AppConfig config = AppConfig.DEFAULT;
        assertNull(config.apiKey());
        assertEquals("claude-sonnet-4-20250514", config.model());
        assertEquals(16384, config.maxTokens());
        assertNull(config.maxTurns());
        assertNull(config.maxBudgetUsd());
        assertEquals("default", config.permissionMode());
        assertFalse(config.verbose());
        assertNull(config.systemPrompt());
    }

    @Test
    void mergeOverridesNonNullFields() {
        AppConfig base = AppConfig.DEFAULT;
        AppConfig overlay = new AppConfig("sk-test", "opus", null, 10, null, null, false, null);

        AppConfig merged = base.merge(overlay);
        assertEquals("sk-test", merged.apiKey());
        assertEquals("opus", merged.model());
        assertEquals(16384, merged.maxTokens()); // kept from base
        assertEquals(10, merged.maxTurns());
    }

    @Test
    void mergeNullOverlayReturnsThis() {
        AppConfig config = AppConfig.DEFAULT;
        assertSame(config, config.merge(null));
    }

    @Test
    void mergeVerboseIsOr() {
        AppConfig base = new AppConfig(null, null, null, null, null, null, true, null);
        AppConfig overlay = new AppConfig(null, null, null, null, null, null, false, null);
        assertTrue(base.merge(overlay).verbose());
    }

    @Test
    void mergeChaining() {
        AppConfig config = AppConfig.DEFAULT
            .merge(new AppConfig("key1", null, null, null, null, null, false, null))
            .merge(new AppConfig(null, "opus", null, null, null, null, false, null))
            .merge(new AppConfig(null, null, 8192, null, null, null, false, null));

        assertEquals("key1", config.apiKey());
        assertEquals("opus", config.model());
        assertEquals(8192, config.maxTokens());
    }
}
