package com.claudecode.services.tips;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TipScheduler {

    private final TipRegistry registry;
    private final TipHistory history;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTips;
    private final Duration defaultInterval;
    private volatile boolean paused;

    public TipScheduler(TipRegistry registry, TipHistory history) {
        this(registry, history, Duration.ofMinutes(5));
    }

    public TipScheduler(TipRegistry registry, TipHistory history, Duration defaultInterval) {
        this.registry = registry;
        this.history = history;
        this.defaultInterval = defaultInterval;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduledTips = new ConcurrentHashMap<>();
        this.paused = false;
    }

    public void scheduleRecurringTips() {
        for (TipRegistry.TipEntry tip : registry.getAllTips()) {
            if (tip.priority() > 0) {
                scheduleTip(tip.id(), defaultInterval);
            }
        }
    }

    public void scheduleTip(String tipId, Duration interval) {
        if (scheduledTips.containsKey(tipId)) {
            cancelTip(tipId);
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> {
                if (!paused && !history.wasShownRecently(tipId)) {
                    TipRegistry.TipEntry tip = registry.getTip(tipId);
                    if (tip != null) {
                        // Notify listeners to display the tip
                    }
                }
            },
            interval.toMillis(),
            interval.toMillis(),
            TimeUnit.MILLISECONDS
        );

        scheduledTips.put(tipId, future);
    }

    public void cancelTip(String tipId) {
        ScheduledFuture<?> future = scheduledTips.remove(tipId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void cancelAllTips() {
        for (ScheduledFuture<?> future : scheduledTips.values()) {
            future.cancel(false);
        }
        scheduledTips.clear();
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public List<String> getScheduledTipIds() {
        return List.copyOf(scheduledTips.keySet());
    }

    public void shutdown() {
        cancelAllTips();
        scheduler.shutdown();
    }

    public TipRegistry getRegistry() {
        return registry;
    }

    public TipHistory getHistory() {
        return history;
    }
}