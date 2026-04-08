package com.claudecode.services.sync;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for settings synchronization across devices/sessions.
 */
public interface SettingsSyncService {

    /**
     * Pushes local settings to the remote sync server.
     */
    boolean push(Map<String, Object> settings);

    /**
     * Pulls settings from the remote sync server.
     */
    Optional<Map<String, Object>> pull();

    /**
     * Returns the last sync timestamp, or empty if never synced.
     */
    Optional<Long> lastSyncTimestamp();

    /**
     * Returns true if sync is enabled and configured.
     */
    boolean isEnabled();
}
