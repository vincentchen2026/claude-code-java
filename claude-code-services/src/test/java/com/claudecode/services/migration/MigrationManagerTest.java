package com.claudecode.services.migration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MigrationManagerTest {

    // --- Model migration chain ---

    @Test
    void migrateFennecToOpus() {
        var store = new SettingsStore(Map.of("model", "claude-fennec"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        // fennec → opus → (opus1m skipped, no flag) stays opus-4
        assertEquals("claude-opus-4", store.getModel());
    }

    @Test
    void migrateLegacyOpusToCurrent() {
        var store = new SettingsStore(Map.of("model", "claude-opus-3"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals("claude-opus-4", store.getModel());
    }

    @Test
    void migrateOpusToOpus1mWhenEligible() {
        var store = new SettingsStore(Map.of("model", "claude-opus-4", "opus1m_eligible", true));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals("claude-opus-4-1m", store.getModel());
    }

    @Test
    void opusStaysWhenNotEligibleFor1m() {
        var store = new SettingsStore(Map.of("model", "claude-opus-4"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals("claude-opus-4", store.getModel());
    }

    @Test
    void migrateSonnet1mToSonnet46ViaChain() {
        var store = new SettingsStore(Map.of("model", "claude-sonnet-4-1m"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        // sonnet-4-1m → sonnet-4-5 → sonnet-4-6
        assertEquals("claude-sonnet-4-6", store.getModel());
    }

    @Test
    void migrateSonnet45ToSonnet46() {
        var store = new SettingsStore(Map.of("model", "claude-sonnet-4-5"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals("claude-sonnet-4-6", store.getModel());
    }

    @Test
    void noMigrationNeededForCurrentModel() {
        var store = new SettingsStore(Map.of("model", "claude-sonnet-4-6"));
        var mgr = new MigrationManager();
        int count = mgr.runMigrations(store);
        assertEquals(0, count);
        assertEquals("claude-sonnet-4-6", store.getModel());
    }

    // --- Settings migrations ---

    @Test
    void migrateAutoUpdatesToSettings() {
        var store = new SettingsStore(Map.of("autoUpdates", true));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals(true, store.getBoolean("settings.autoUpdates").orElse(false));
        assertFalse(store.containsKey("autoUpdates"));
    }

    @Test
    void migrateBypassPermissionsToSettings() {
        var store = new SettingsStore(Map.of("bypassPermissions", true));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals(true, store.getBoolean("settings.bypassPermissions").orElse(false));
        assertFalse(store.containsKey("bypassPermissions"));
    }

    @Test
    void migrateMcpServersToSettings() {
        var store = new SettingsStore(Map.of("mcpServers", "server-config"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals("server-config", store.getString("settings.mcpServers").orElse(""));
        assertFalse(store.containsKey("mcpServers"));
    }

    @Test
    void migrateReplBridgeToRemoteControl() {
        var store = new SettingsStore(Map.of("replBridge", true));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertEquals(true, store.getBoolean("settings.remoteControl").orElse(false));
        assertFalse(store.containsKey("replBridge"));
    }

    @Test
    void settingsMigrationSkippedWhenAlreadyMigrated() {
        var store = new SettingsStore(Map.of(
            "autoUpdates", true,
            "settings.autoUpdates", true
        ));
        var mgr = new MigrationManager();
        int count = mgr.runMigrations(store);
        // autoUpdates migration should be skipped since settings.autoUpdates already exists
        assertFalse(mgr.getExecutedMigrations().contains("auto-updates-to-settings"));
    }

    // --- MigrationManager lifecycle ---

    @Test
    void executedMigrationsTracked() {
        var store = new SettingsStore(Map.of("model", "claude-fennec"));
        var mgr = new MigrationManager();
        mgr.runMigrations(store);
        assertTrue(mgr.getExecutedMigrations().contains("fennec-to-opus"));
    }

    @Test
    void runMigrationsReturnsCount() {
        var store = new SettingsStore(Map.of("model", "claude-sonnet-4-5"));
        var mgr = new MigrationManager();
        int count = mgr.runMigrations(store);
        assertEquals(1, count); // only sonnet-45-to-sonnet-46
    }

    @Test
    void customMigrationList() {
        Migration custom = new Migration() {
            @Override public String name() { return "custom"; }
            @Override public boolean needsMigration(SettingsStore s) { return true; }
            @Override public void execute(SettingsStore s) { s.set("custom", "done"); }
        };
        var mgr = new MigrationManager(List.of(custom));
        var store = new SettingsStore();
        mgr.runMigrations(store);
        assertEquals("done", store.getString("custom").orElse(""));
    }

    @Test
    void getMigrationsReturnsAllRegistered() {
        var mgr = new MigrationManager();
        assertEquals(9, mgr.getMigrations().size());
    }

    // --- SettingsStore ---

    @Test
    void settingsStoreBasicOperations() {
        var store = new SettingsStore();
        store.set("key", "value");
        assertEquals("value", store.getString("key").orElse(""));
        assertTrue(store.containsKey("key"));
        store.remove("key");
        assertFalse(store.containsKey("key"));
    }

    @Test
    void settingsStoreAsMapIsImmutable() {
        var store = new SettingsStore(Map.of("a", "b"));
        var map = store.asMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", "d"));
    }
}
