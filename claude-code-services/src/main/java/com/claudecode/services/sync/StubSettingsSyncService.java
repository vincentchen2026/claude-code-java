package com.claudecode.services.sync;

import java.util.Map;
import java.util.Optional;

/**
 * Stub implementation of SettingsSyncService.
 * Always reports as disabled; push/pull are no-ops.
 */
public class StubSettingsSyncService implements SettingsSyncService {

    private final boolean enabled;

    public StubSettingsSyncService() {
        this(false);
    }

    public StubSettingsSyncService(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean push(Map<String, Object> settings) {
        return false; // Stub: no remote server
    }

    @Override
    public Optional<Map<String, Object>> pull() {
        return Optional.empty(); // Stub: no remote server
    }

    @Override
    public Optional<Long> lastSyncTimestamp() {
        return Optional.empty();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
