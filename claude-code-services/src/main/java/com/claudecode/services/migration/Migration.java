package com.claudecode.services.migration;

/**
 * Interface for a single migration step.
 */
public interface Migration {

    /**
     * Returns a unique name for this migration.
     */
    String name();

    /**
     * Checks whether this migration needs to be applied.
     */
    boolean needsMigration(SettingsStore settings);

    /**
     * Executes the migration.
     */
    void execute(SettingsStore settings);
}
