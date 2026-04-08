package com.claudecode.services.compact;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CompactWarningState {

    private final Map<String, WarningEntry> warnings;
    private final AtomicInteger totalWarnings;

    public CompactWarningState() {
        this.warnings = new ConcurrentHashMap<>();
        this.totalWarnings = new AtomicInteger(0);
    }

    public void recordWarning(String code, String message, CompactWarningHook.WarningCategory category) {
        warnings.put(code, new WarningEntry(
            code,
            message,
            category,
            System.currentTimeMillis(),
            1
        ));
        totalWarnings.incrementAndGet();
    }

    public void incrementWarning(String code) {
        WarningEntry existing = warnings.get(code);
        if (existing != null) {
            warnings.put(code, new WarningEntry(
                existing.code(),
                existing.message(),
                existing.category(),
                existing.firstSeen(),
                existing.count() + 1
            ));
        }
    }

    public WarningEntry getWarning(String code) {
        return warnings.get(code);
    }

    public Map<String, WarningEntry> getAllWarnings() {
        return Map.copyOf(warnings);
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public int getTotalWarningCount() {
        return totalWarnings.get();
    }

    public void clear() {
        warnings.clear();
    }

    public void clearCategory(CompactWarningHook.WarningCategory category) {
        warnings.entrySet().removeIf(e -> e.getValue().category() == category);
    }

    public record WarningEntry(
        String code,
        String message,
        CompactWarningHook.WarningCategory category,
        long firstSeen,
        int count
    ) {}
}