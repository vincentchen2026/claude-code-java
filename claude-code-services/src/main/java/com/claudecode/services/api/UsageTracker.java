package com.claudecode.services.api;

import com.claudecode.core.message.Usage;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UsageTracker {

    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    private final AtomicLong totalCachedTokens = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalCost = new AtomicLong(0);

    private final ConcurrentHashMap<String, AtomicLong> modelUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> toolUsage = new ConcurrentHashMap<>();

    private volatile Instant sessionStart;
    private volatile String currentModel;

    public UsageTracker() {
        this.sessionStart = Instant.now();
    }

    public void recordUsage(Usage usage, String model) {
        if (usage == null) return;

        totalInputTokens.addAndGet(usage.inputTokens());
        totalOutputTokens.addAndGet(usage.outputTokens());
        totalCachedTokens.addAndGet(usage.cacheCreationInputTokens() + usage.cacheReadInputTokens());
        totalRequests.incrementAndGet();
        currentModel = model;

        modelUsage.computeIfAbsent(model, k -> new AtomicLong(0))
            .addAndGet(usage.inputTokens() + usage.outputTokens());
    }

    public void recordToolUse(String toolName) {
        if (toolName == null) return;
        toolUsage.computeIfAbsent(toolName, k -> new AtomicLong(0))
            .incrementAndGet();
    }

    public void recordError() {
        totalErrors.incrementAndGet();
    }

    public void recordCost(long costUnits) {
        totalCost.addAndGet(costUnits);
    }

    public UsageSnapshot getSnapshot() {
        return new UsageSnapshot(
            totalInputTokens.get(),
            totalOutputTokens.get(),
            totalCachedTokens.get(),
            totalRequests.get(),
            totalErrors.get(),
            totalCost.get(),
            Map.copyOf(modelUsage),
            Map.copyOf(toolUsage),
            sessionStart,
            Instant.now(),
            currentModel
        );
    }

    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalCachedTokens.set(0);
        totalRequests.set(0);
        totalErrors.set(0);
        totalCost.set(0);
        modelUsage.clear();
        toolUsage.clear();
        sessionStart = Instant.now();
    }

    public long getTotalInputTokens() {
        return totalInputTokens.get();
    }

    public long getTotalOutputTokens() {
        return totalOutputTokens.get();
    }

    public long getTotalCachedTokens() {
        return totalCachedTokens.get();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }

    public long getTotalCost() {
        return totalCost.get();
    }

    public record UsageSnapshot(
        long inputTokens,
        long outputTokens,
        long cachedTokens,
        long requestCount,
        long errorCount,
        long totalCost,
        Map<String, AtomicLong> usageByModel,
        Map<String, AtomicLong> usageByTool,
        Instant sessionStart,
        Instant sessionEnd,
        String currentModel
    ) {
        public long totalTokens() {
            return inputTokens + outputTokens;
        }
    }
}