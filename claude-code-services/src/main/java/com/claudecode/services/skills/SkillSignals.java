package com.claudecode.services.skills;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SkillSignals {

    private final Map<String, CopyOnWriteArrayList<SignalListener>> listeners;
    private final Map<String, SignalHistory> history;

    public SkillSignals() {
        this.listeners = new ConcurrentHashMap<>();
        this.history = new ConcurrentHashMap<>();
    }

    public void emit(String skillId, SignalType type, String message) {
        Signal signal = new Signal(skillId, type, message, Instant.now());
        history.computeIfAbsent(skillId, k -> new SignalHistory()).add(signal);

        CopyOnWriteArrayList<SignalListener> skillListeners = listeners.get(skillId);
        if (skillListeners != null) {
            for (SignalListener listener : skillListeners) {
                try {
                    listener.onSignal(signal);
                } catch (Exception e) {
                    // Log but don't fail
                }
            }
        }
    }

    public void emitGlobal(SignalType type, String message) {
        for (Map.Entry<String, CopyOnWriteArrayList<SignalListener>> entry : listeners.entrySet()) {
            Signal signal = new Signal(entry.getKey(), type, message, Instant.now());
            for (SignalListener listener : entry.getValue()) {
                try {
                    listener.onGlobalSignal(signal);
                } catch (Exception e) {
                    // Log but don't fail
                }
            }
        }
    }

    public void addListener(String skillId, SignalListener listener) {
        listeners.computeIfAbsent(skillId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void removeListener(String skillId, SignalListener listener) {
        CopyOnWriteArrayList<SignalListener> skillListeners = listeners.get(skillId);
        if (skillListeners != null) {
            skillListeners.remove(listener);
        }
    }

    public SignalHistory getHistory(String skillId) {
        return history.getOrDefault(skillId, new SignalHistory());
    }

    public void clearHistory(String skillId) {
        history.remove(skillId);
    }

    public enum SignalType {
        LOADED, UNLOADED, INVOKED, ERROR, TIMEOUT, CACHED
    }

    public record Signal(
        String skillId,
        SignalType type,
        String message,
        Instant timestamp
    ) {}

    public static class SignalHistory extends CopyOnWriteArrayList<Signal> {}

    @FunctionalInterface
    public interface SignalListener {
        void onSignal(Signal signal);
        default void onGlobalSignal(Signal signal) {}
    }
}