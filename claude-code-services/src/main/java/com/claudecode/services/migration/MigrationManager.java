package com.claudecode.services.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages and executes the migration chain at startup.
 * Migrations are run in order; each checks if it needs to apply.
 */
public class MigrationManager {

    private static final Logger log = LoggerFactory.getLogger(MigrationManager.class);

    private static final List<Migration> DEFAULT_MIGRATIONS = List.of(
        // Model migrations
        new MigrateFennecToOpus(),
        new MigrateLegacyOpusToCurrent(),
        new MigrateOpusToOpus1m(),
        new MigrateSonnet1mToSonnet45(),
        new MigrateSonnet45ToSonnet46(),
        // Settings migrations
        new MigrateAutoUpdatesToSettings(),
        new MigrateBypassPermissionsToSettings(),
        new MigrateMcpServersToSettings(),
        new MigrateReplBridgeToRemoteControl()
    );

    private final List<Migration> migrations;
    private final List<String> executedMigrations = new ArrayList<>();

    public MigrationManager() {
        this(DEFAULT_MIGRATIONS);
    }

    public MigrationManager(List<Migration> migrations) {
        this.migrations = List.copyOf(migrations);
    }

    /**
     * Runs all pending migrations in order.
     * Returns the number of migrations executed.
     */
    public int runMigrations(SettingsStore settings) {
        int count = 0;
        for (Migration migration : migrations) {
            if (migration.needsMigration(settings)) {
                log.info("Running migration: {}", migration.name());
                migration.execute(settings);
                executedMigrations.add(migration.name());
                count++;
            }
        }
        if (count > 0) {
            log.info("Completed {} migration(s)", count);
        }
        return count;
    }

    /**
     * Returns the list of migrations that were executed.
     */
    public List<String> getExecutedMigrations() {
        return Collections.unmodifiableList(executedMigrations);
    }

    /**
     * Returns all registered migrations.
     */
    public List<Migration> getMigrations() {
        return migrations;
    }

    // --- Model Migrations ---

    static class MigrateFennecToOpus implements Migration {
        @Override public String name() { return "fennec-to-opus"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return "claude-fennec".equals(s.getModel());
        }
        @Override public void execute(SettingsStore s) {
            s.setModel("claude-opus-4");
        }
    }

    static class MigrateLegacyOpusToCurrent implements Migration {
        @Override public String name() { return "legacy-opus-to-current"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return "claude-opus-3".equals(s.getModel());
        }
        @Override public void execute(SettingsStore s) {
            s.setModel("claude-opus-4");
        }
    }

    static class MigrateOpusToOpus1m implements Migration {
        @Override public String name() { return "opus-to-opus-1m"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return "claude-opus-4".equals(s.getModel())
                && s.getBoolean("opus1m_eligible").orElse(false);
        }
        @Override public void execute(SettingsStore s) {
            s.setModel("claude-opus-4-1m");
        }
    }

    static class MigrateSonnet1mToSonnet45 implements Migration {
        @Override public String name() { return "sonnet-1m-to-sonnet-45"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return "claude-sonnet-4-1m".equals(s.getModel());
        }
        @Override public void execute(SettingsStore s) {
            s.setModel("claude-sonnet-4-5");
        }
    }

    static class MigrateSonnet45ToSonnet46 implements Migration {
        @Override public String name() { return "sonnet-45-to-sonnet-46"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return "claude-sonnet-4-5".equals(s.getModel());
        }
        @Override public void execute(SettingsStore s) {
            s.setModel("claude-sonnet-4-6");
        }
    }

    // --- Settings Migrations ---

    static class MigrateAutoUpdatesToSettings implements Migration {
        @Override public String name() { return "auto-updates-to-settings"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return s.containsKey("autoUpdates") && !s.containsKey("settings.autoUpdates");
        }
        @Override public void execute(SettingsStore s) {
            s.getBoolean("autoUpdates").ifPresent(v -> {
                s.set("settings.autoUpdates", v);
                s.remove("autoUpdates");
            });
        }
    }

    static class MigrateBypassPermissionsToSettings implements Migration {
        @Override public String name() { return "bypass-permissions-to-settings"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return s.containsKey("bypassPermissions") && !s.containsKey("settings.bypassPermissions");
        }
        @Override public void execute(SettingsStore s) {
            s.getBoolean("bypassPermissions").ifPresent(v -> {
                s.set("settings.bypassPermissions", v);
                s.remove("bypassPermissions");
            });
        }
    }

    static class MigrateMcpServersToSettings implements Migration {
        @Override public String name() { return "mcp-servers-to-settings"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return s.containsKey("mcpServers") && !s.containsKey("settings.mcpServers");
        }
        @Override public void execute(SettingsStore s) {
            Object val = s.getString("mcpServers").orElse(null);
            if (val != null) {
                s.set("settings.mcpServers", val);
                s.remove("mcpServers");
            }
        }
    }

    static class MigrateReplBridgeToRemoteControl implements Migration {
        @Override public String name() { return "repl-bridge-to-remote-control"; }
        @Override public boolean needsMigration(SettingsStore s) {
            return s.containsKey("replBridge") && !s.containsKey("settings.remoteControl");
        }
        @Override public void execute(SettingsStore s) {
            s.getBoolean("replBridge").ifPresent(v -> {
                s.set("settings.remoteControl", v);
                s.remove("replBridge");
            });
        }
    }
}
