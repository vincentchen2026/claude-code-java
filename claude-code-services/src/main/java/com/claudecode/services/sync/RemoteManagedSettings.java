package com.claudecode.services.sync;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for remote managed settings (organization-level policy enforcement).
 */
public interface RemoteManagedSettings {

    /**
     * Fetches managed settings from the remote server.
     */
    Optional<Map<String, Object>> fetchManagedSettings();

    /**
     * Returns true if a specific setting is managed (locked by admin).
     */
    boolean isManaged(String key);

    /**
     * Returns the managed value for a key, if it exists.
     */
    Optional<Object> getManagedValue(String key);

    /**
     * Returns true if remote management is enabled.
     */
    boolean isEnabled();
}
