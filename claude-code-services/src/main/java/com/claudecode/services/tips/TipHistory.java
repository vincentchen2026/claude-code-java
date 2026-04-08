package com.claudecode.services.tips;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TipHistory {

    private final Map<String, List<TipShownRecord>> shownRecords;
    private final Duration recentWindow;

    public TipHistory() {
        this(Duration.ofHours(24));
    }

    public TipHistory(Duration recentWindow) {
        this.shownRecords = new ConcurrentHashMap<>();
        this.recentWindow = recentWindow;
    }

    public void recordShown(String tipId) {
        recordShown(tipId, Instant.now());
    }

    public void recordShown(String tipId, Instant shownAt) {
        shownRecords.computeIfAbsent(tipId, k -> new CopyOnWriteArrayList<>())
            .add(new TipShownRecord(tipId, shownAt));
    }

    public void recordDismissed(String tipId) {
        shownRecords.computeIfAbsent(tipId, k -> new CopyOnWriteArrayList<>())
            .add(new TipShownRecord(tipId, Instant.now(), true));
    }

    public boolean wasShownRecently(String tipId) {
        List<TipShownRecord> records = shownRecords.get(tipId);
        if (records == null || records.isEmpty()) {
            return false;
        }

        Instant cutoff = Instant.now().minus(recentWindow);
        return records.stream()
            .anyMatch(r -> !r.dismissed() && r.shownAt().isAfter(cutoff));
    }

    public int getShownCount(String tipId) {
        List<TipShownRecord> records = shownRecords.get(tipId);
        if (records == null) {
            return 0;
        }
        return (int) records.stream().filter(r -> !r.dismissed()).count();
    }

    public int getDismissedCount(String tipId) {
        List<TipShownRecord> records = shownRecords.get(tipId);
        if (records == null) {
            return 0;
        }
        return (int) records.stream().filter(TipShownRecord::dismissed).count();
    }

    public List<TipShownRecord> getRecentRecords(String tipId) {
        List<TipShownRecord> records = shownRecords.get(tipId);
        if (records == null) {
            return List.of();
        }

        Instant cutoff = Instant.now().minus(recentWindow);
        return records.stream()
            .filter(r -> r.shownAt().isAfter(cutoff))
            .toList();
    }

    public void clearOldRecords() {
        Instant cutoff = Instant.now().minus(recentWindow.multipliedBy(7));
        
        for (Map.Entry<String, List<TipShownRecord>> entry : shownRecords.entrySet()) {
            entry.getValue().removeIf(r -> r.shownAt().isBefore(cutoff));
            if (entry.getValue().isEmpty()) {
                shownRecords.remove(entry.getKey());
            }
        }
    }

    public void clearAll() {
        shownRecords.clear();
    }

    public int getTotalTipsShown() {
        return shownRecords.values().stream()
            .mapToInt(list -> (int) list.stream().filter(r -> !r.dismissed()).count())
            .sum();
    }

    public record TipShownRecord(
        String tipId,
        Instant shownAt,
        boolean dismissed
    ) {
        public TipShownRecord(String tipId, Instant shownAt) {
            this(tipId, shownAt, false);
        }
    }
}