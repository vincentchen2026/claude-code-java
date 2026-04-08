package com.claudecode.services.migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple settings store used by migrations to read/write settings.
 */
public class SettingsStore {

    private final Map<String, Object> settings;

    public SettingsStore() {
        this.settings = new HashMap<>();
    }

    public SettingsStore(Map<String, Object> initial) {
        this.settings = new HashMap<>(initial);
    }

    public Optional<String> getString(String key) {
        Object val = settings.get(key);
        return val instanceof String s ? Optional.of(s) : Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        Object val = settings.get(key);
        return val instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    public void set(String key, Object value) {
        settings.put(key, value);
    }

    public void remove(String key) {
        settings.remove(key);
    }

    public boolean containsKey(String key) {
        return settings.containsKey(key);
    }

    public String getModel() {
        return getString("model").orElse("");
    }

    public void setModel(String model) {
        set("model", model);
    }

    public Map<String, Object> asMap() {
        return Map.copyOf(settings);
    }
}
