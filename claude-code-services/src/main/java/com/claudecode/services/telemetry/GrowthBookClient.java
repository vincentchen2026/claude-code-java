package com.claudecode.services.telemetry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GrowthBook feature flag integration stub.
 * Provides feature flag evaluation with local overrides.
 */
public class GrowthBookClient {

    private final Map<String, Object> overrides = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public GrowthBookClient(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Evaluates a boolean feature flag.
     */
    public boolean isFeatureEnabled(String featureKey) {
        if (!enabled) return false;
        Object override = overrides.get(featureKey);
        if (override instanceof Boolean b) return b;
        // Stub: would call GrowthBook SDK
        return false;
    }

    /**
     * Evaluates a feature flag with a string value.
     */
    public Optional<String> getFeatureValue(String featureKey) {
        if (!enabled) return Optional.empty();
        Object override = overrides.get(featureKey);
        if (override != null) return Optional.of(override.toString());
        // Stub: would call GrowthBook SDK
        return Optional.empty();
    }

    /**
     * Sets a local override for testing.
     */
    public void setOverride(String featureKey, Object value) {
        overrides.put(featureKey, value);
    }

    /**
     * Clears all local overrides.
     */
    public void clearOverrides() {
        overrides.clear();
    }

    /**
     * Returns whether the client is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the client.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
