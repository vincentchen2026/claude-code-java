package com.claudecode.services.sync;

import java.util.Map;
import java.util.Optional;

/**
 * Stub implementation of RemoteManagedSettings.
 * Always reports as disabled; no managed settings.
 */
public class StubRemoteManagedSettings implements RemoteManagedSettings {

    private final boolean enabled;

    public StubRemoteManagedSettings() {
        this(false);
    }

    public StubRemoteManagedSettings(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Optional<Map<String, Object>> fetchManagedSettings() {
        return Optional.empty();
    }

    @Override
    public boolean isManaged(String key) {
        return false;
    }

    @Override
    public Optional<Object> getManagedValue(String key) {
        return Optional.empty();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
