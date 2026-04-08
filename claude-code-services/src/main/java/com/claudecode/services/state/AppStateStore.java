package com.claudecode.services.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class AppStateStore {

    private static final Logger log = LoggerFactory.getLogger(AppStateStore.class);

    private final Map<String, StateEntry> state = new ConcurrentHashMap<>();
    private final Map<String, Observer> observers = new ConcurrentHashMap<>();

    public <T> void set(String key, T value) {
        StateEntry oldEntry = state.get(key);
        state.put(key, new StateEntry(key, value, oldEntry != null ? oldEntry.version() + 1 : 1));

        notifyObservers(key, value);
        log.debug("State updated: {} (version {})", key, state.get(key).version());
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        StateEntry entry = state.get(key);
        return entry != null ? (T) entry.value() : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        StateEntry entry = state.get(key);
        return entry != null ? (T) entry.value() : defaultValue;
    }

    public <T> T select(Function<Map<String, StateEntry>, T> selector) {
        return selector.apply(state);
    }

    public void remove(String key) {
        state.remove(key);
        log.debug("State removed: {}", key);
    }

    public void clear() {
        state.clear();
        log.info("State cleared");
    }

    public void subscribe(String key, Observer observer) {
        observers.put(key, observer);
    }

    public void unsubscribe(String key) {
        observers.remove(key);
    }

    private void notifyObservers(String key, Object value) {
        Observer observer = observers.get(key);
        if (observer != null) {
            try {
                observer.onChange(key, value);
            } catch (Exception e) {
                log.error("Observer error for key {}: {}", key, e.getMessage());
            }
        }
    }

    public Map<String, StateEntry> getAll() {
        return Map.copyOf(state);
    }

    public int size() {
        return state.size();
    }

    public record StateEntry(
        String key,
        Object value,
        int version
    ) {}

    public interface Observer {
        void onChange(String key, Object value);
    }

    public static class Selectors {
        public static String agentId(Map<String, StateEntry> state) {
            StateEntry entry = state.get("agentId");
            return entry != null ? (String) entry.value() : null;
        }

        public static boolean isConnected(Map<String, StateEntry> state) {
            StateEntry entry = state.get("connected");
            return entry != null && Boolean.TRUE.equals(entry.value());
        }

        public static String currentModel(Map<String, StateEntry> state) {
            StateEntry entry = state.get("currentModel");
            return entry != null ? (String) entry.value() : "claude-sonnet-4-20250514";
        }
    }
}