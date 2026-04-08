package com.claudecode.services.skills;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SkillTelemetry {

    private final Map<String, SkillMetrics> metrics;
    private final Map<String, AtomicLong> invocationCount;
    private final Map<String, AtomicLong> errorCount;

    public SkillTelemetry() {
        this.metrics = new ConcurrentHashMap<>();
        this.invocationCount = new ConcurrentHashMap<>();
        this.errorCount = new ConcurrentHashMap<>();
    }

    public void recordInvocation(String skillId, Duration duration, boolean success) {
        metrics.computeIfAbsent(skillId, k -> new SkillMetrics())
            .recordInvocation(duration, success);

        invocationCount.computeIfAbsent(skillId, k -> new AtomicLong(0))
            .incrementAndGet();

        if (!success) {
            errorCount.computeIfAbsent(skillId, k -> new AtomicLong(0))
                .incrementAndGet();
        }
    }

    public void recordInvocationStart(String skillId) {
        metrics.computeIfAbsent(skillId, k -> new SkillMetrics())
            .recordStart(Instant.now());
    }

    public void recordInvocationEnd(String skillId, boolean success) {
        SkillMetrics m = metrics.get(skillId);
        if (m != null) {
            m.recordEnd(Instant.now(), success);
        }
    }

    public SkillMetrics getMetrics(String skillId) {
        return metrics.getOrDefault(skillId, new SkillMetrics());
    }

    public long getTotalInvocations(String skillId) {
        return invocationCount.getOrDefault(skillId, new AtomicLong(0)).get();
    }

    public long getTotalErrors(String skillId) {
        return errorCount.getOrDefault(skillId, new AtomicLong(0)).get();
    }

    public double getSuccessRate(String skillId) {
        long total = getTotalInvocations(skillId);
        if (total == 0) return 1.0;
        long errors = getTotalErrors(skillId);
        return (double) (total - errors) / total;
    }

    public Map<String, SkillMetrics> getAllMetrics() {
        return Map.copyOf(metrics);
    }

    public void reset(String skillId) {
        metrics.remove(skillId);
        invocationCount.remove(skillId);
        errorCount.remove(skillId);
    }

    public void resetAll() {
        metrics.clear();
        invocationCount.clear();
        errorCount.clear();
    }

    public static class SkillMetrics {
        private final AtomicLong totalInvocations = new AtomicLong(0);
        private final AtomicLong totalErrors = new AtomicLong(0);
        private final AtomicLong totalDurationMs = new AtomicLong(0);
        private volatile Instant lastInvocation;

        public void recordInvocation(Duration duration, boolean success) {
            totalInvocations.incrementAndGet();
            if (!success) {
                totalErrors.incrementAndGet();
            }
            totalDurationMs.addAndGet(duration.toMillis());
            lastInvocation = Instant.now();
        }

        public void recordStart(Instant start) {
            lastInvocation = start;
        }

        public void recordEnd(Instant end, boolean success) {
            totalInvocations.incrementAndGet();
            if (!success) {
                totalErrors.incrementAndGet();
            }
            if (lastInvocation != null) {
                totalDurationMs.addAndGet(Duration.between(lastInvocation, end).toMillis());
            }
        }

        public long getTotalInvocations() {
            return totalInvocations.get();
        }

        public long getTotalErrors() {
            return totalErrors.get();
        }

        public double getAverageDurationMs() {
            long total = totalInvocations.get();
            if (total == 0) return 0;
            return (double) totalDurationMs.get() / total;
        }

        public Instant getLastInvocation() {
            return lastInvocation;
        }
    }
}