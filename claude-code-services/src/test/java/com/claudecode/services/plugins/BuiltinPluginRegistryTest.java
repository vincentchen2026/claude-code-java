package com.claudecode.services.plugins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinPluginRegistryTest {

    private BuiltinPluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BuiltinPluginRegistry();
    }

    @Test
    void registerPlugin() {
        BuiltinPlugin plugin = new BuiltinPlugin("test", "A test plugin", true);
        registry.register(plugin);

        assertEquals(1, registry.getAllPlugins().size());
        assertEquals("test@builtin", registry.getAllPlugins().get(0).id());
    }

    @Test
    void enabledByDefaultPlugin() {
        BuiltinPlugin plugin = new BuiltinPlugin("auto", "Auto-enabled", true);
        registry.register(plugin);

        assertTrue(registry.isEnabled("auto@builtin"));
    }

    @Test
    void disabledByDefaultPlugin() {
        BuiltinPlugin plugin = new BuiltinPlugin("manual", "Manual-enable", false);
        registry.register(plugin);

        assertFalse(registry.isEnabled("manual@builtin"));
    }

    @Test
    void enablePlugin() {
        BuiltinPlugin plugin = new BuiltinPlugin("test", "Test", false);
        registry.register(plugin);

        assertTrue(registry.enable("test@builtin"));
        assertTrue(registry.isEnabled("test@builtin"));
    }

    @Test
    void disablePlugin() {
        BuiltinPlugin plugin = new BuiltinPlugin("test", "Test", true);
        registry.register(plugin);

        assertTrue(registry.disable("test@builtin"));
        assertFalse(registry.isEnabled("test@builtin"));
    }

    @Test
    void enableNonexistentPlugin() {
        assertFalse(registry.enable("nonexistent@builtin"));
    }

    @Test
    void disableNonexistentPlugin() {
        assertFalse(registry.disable("nonexistent@builtin"));
    }

    @Test
    void getPlugin() {
        BuiltinPlugin plugin = new BuiltinPlugin("test", "Test", true);
        registry.register(plugin);

        assertTrue(registry.getPlugin("test@builtin").isPresent());
        assertFalse(registry.getPlugin("missing@builtin").isPresent());
    }

    @Test
    void getEnabledPlugins() {
        registry.register(new BuiltinPlugin("enabled1", "E1", true));
        registry.register(new BuiltinPlugin("enabled2", "E2", true));
        registry.register(new BuiltinPlugin("disabled1", "D1", false));

        List<BuiltinPlugin> enabled = registry.getEnabledPlugins();
        assertEquals(2, enabled.size());
    }

    @Test
    void getDisabledPlugins() {
        registry.register(new BuiltinPlugin("enabled1", "E1", true));
        registry.register(new BuiltinPlugin("disabled1", "D1", false));

        List<BuiltinPlugin> disabled = registry.getDisabledPlugins();
        assertEquals(1, disabled.size());
        assertEquals("disabled1", disabled.get(0).name());
    }

    @Test
    void handlePluginCommandList() {
        registry.register(new BuiltinPlugin("test", "A test plugin", true));

        String result = registry.handlePluginCommand("list");
        assertTrue(result.contains("test@builtin"));
        assertTrue(result.contains("enabled"));
    }

    @Test
    void handlePluginCommandEnable() {
        registry.register(new BuiltinPlugin("test", "Test", false));

        String result = registry.handlePluginCommand("enable test@builtin");
        assertTrue(result.contains("Enabled"));
        assertTrue(registry.isEnabled("test@builtin"));
    }

    @Test
    void handlePluginCommandDisable() {
        registry.register(new BuiltinPlugin("test", "Test", true));

        String result = registry.handlePluginCommand("disable test@builtin");
        assertTrue(result.contains("Disabled"));
        assertFalse(registry.isEnabled("test@builtin"));
    }

    @Test
    void handlePluginCommandEmpty() {
        registry.register(new BuiltinPlugin("test", "Test", true));

        String result = registry.handlePluginCommand("");
        assertTrue(result.contains("test@builtin"));
    }

    @Test
    void handlePluginCommandUnknownAction() {
        String result = registry.handlePluginCommand("restart test@builtin");
        assertTrue(result.contains("Unknown action"));
    }

    @Test
    void handlePluginCommandNoPlugins() {
        String result = registry.handlePluginCommand("list");
        assertTrue(result.contains("No plugins"));
    }

    @Test
    void pluginIdFormat() {
        BuiltinPlugin plugin = new BuiltinPlugin("my-plugin", "desc", true);
        assertEquals("my-plugin@builtin", plugin.id());
    }
}
