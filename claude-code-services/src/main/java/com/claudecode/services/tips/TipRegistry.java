package com.claudecode.services.tips;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TipRegistry {

    private final Map<String, TipEntry> tips;
    private final List<TipCategory> categories;
    private final List<TipLifecycleListener> listeners;

    public TipRegistry() {
        this.tips = new ConcurrentHashMap<>();
        this.categories = new CopyOnWriteArrayList<>();
        this.categories.add(TipCategory.GENERAL);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void registerTip(TipEntry tip) {
        tips.put(tip.id(), tip);
        notifyListeners(tip, TipEvent.REGISTERED);
    }

    public void unregisterTip(String tipId) {
        TipEntry removed = tips.remove(tipId);
        if (removed != null) {
            notifyListeners(removed, TipEvent.UNREGISTERED);
        }
    }

    public TipEntry getTip(String tipId) {
        return tips.get(tipId);
    }

    public List<TipEntry> getTipsByCategory(TipCategory category) {
        return tips.values().stream()
            .filter(t -> t.category() == category)
            .toList();
    }

    public List<TipEntry> getTipsByTag(String tag) {
        return tips.values().stream()
            .filter(t -> t.tags().contains(tag))
            .toList();
    }

    public List<TipEntry> getAllTips() {
        return List.copyOf(tips.values());
    }

    public void addCategory(TipCategory category) {
        if (!categories.contains(category)) {
            categories.add(category);
        }
    }

    public List<TipCategory> getCategories() {
        return List.copyOf(categories);
    }

    public void addListener(TipLifecycleListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TipLifecycleListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(TipEntry tip, TipEvent event) {
        for (TipLifecycleListener listener : listeners) {
            try {
                listener.onTipEvent(tip, event);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    public enum TipCategory {
        GENERAL, TOOL, AGENT, KEYBOARD_SHORTCUT, WORKFLOW, DEBUG
    }

    public enum TipEvent {
        REGISTERED, UNREGISTERED, SHOWN, DISMISSED
    }

    public interface TipLifecycleListener {
        void onTipEvent(TipEntry tip, TipEvent event);
    }

    public record TipEntry(
        String id,
        String content,
        TipCategory category,
        List<String> tags,
        int priority
    ) {}
}