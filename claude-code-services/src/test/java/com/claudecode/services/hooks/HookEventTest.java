package com.claudecode.services.hooks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HookEventTest {

    @Test
    void has27Events() {
        assertEquals(27, HookEvent.values().length);
    }

    @Test
    void configKeyIsLowercase() {
        assertEquals("pre_tool_use", HookEvent.PRE_TOOL_USE.configKey());
        assertEquals("session_start", HookEvent.SESSION_START.configKey());
        assertEquals("file_changed", HookEvent.FILE_CHANGED.configKey());
    }

    @Test
    void fromConfigKeyRoundTrips() {
        for (HookEvent event : HookEvent.values()) {
            assertEquals(event, HookEvent.fromConfigKey(event.configKey()));
        }
    }

    @Test
    void fromConfigKeyIsCaseInsensitive() {
        assertEquals(HookEvent.PRE_TOOL_USE, HookEvent.fromConfigKey("PRE_TOOL_USE"));
        assertEquals(HookEvent.PRE_TOOL_USE, HookEvent.fromConfigKey("pre_tool_use"));
    }

    @Test
    void fromConfigKeyThrowsForUnknown() {
        assertThrows(IllegalArgumentException.class, () -> HookEvent.fromConfigKey("unknown"));
    }
}
