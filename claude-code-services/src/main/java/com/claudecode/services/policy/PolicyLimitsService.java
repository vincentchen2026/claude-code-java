package com.claudecode.services.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PolicyLimitsService {

    private static final Logger log = LoggerFactory.getLogger(PolicyLimitsService.class);

    private final Map<String, PolicyLimit> limits = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> usageCounters = new ConcurrentHashMap<>();

    public PolicyLimitsService() {
        initializeDefaultLimits();
    }

    private void initializeDefaultLimits() {
        limits.put("max_tokens_per_request", new PolicyLimit("max_tokens_per_request", 100000, 0, LimitType.TOKEN));
        limits.put("max_requests_per_minute", new PolicyLimit("max_requests_per_minute", 60, 0, LimitType.RATE));
        limits.put("max_concurrent_requests", new PolicyLimit("max_concurrent_requests", 10, 0, LimitType.CONCURRENT));
        limits.put("max_file_size_mb", new PolicyLimit("max_file_size_mb", 100, 0, LimitType.SIZE));
        limits.put("max_context_length", new PolicyLimit("max_context_length", 200000, 0, LimitType.CONTEXT));
    }

    public boolean checkLimit(String limitName, long value) {
        PolicyLimit limit = limits.get(limitName);
        if (limit == null) {
            return true;
        }

        if (value > limit.maxValue()) {
            log.warn("Limit exceeded: {} (value: {} > max: {})", limitName, value, limit.maxValue());
            return false;
        }

        return true;
    }

    public boolean checkAndIncrement(String limitName) {
        PolicyLimit limit = limits.get(limitName);
        if (limit == null) {
            return true;
        }

        AtomicInteger counter = usageCounters.computeIfAbsent(limitName, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > limit.maxValue()) {
            counter.decrementAndGet();
            log.warn("Rate limit exceeded: {} (current: {} > max: {})", limitName, current, limit.maxValue());
            return false;
        }

        return true;
    }

    public void decrement(String limitName) {
        AtomicInteger counter = usageCounters.get(limitName);
        if (counter != null && counter.get() > 0) {
            counter.decrementAndGet();
        }
    }

    public void setLimit(String limitName, long maxValue) {
        PolicyLimit existing = limits.get(limitName);
        if (existing != null) {
            limits.put(limitName, new PolicyLimit(limitName, maxValue, existing.currentValue(), existing.type()));
        } else {
            limits.put(limitName, new PolicyLimit(limitName, maxValue, 0, LimitType.GENERAL));
        }
        log.info("Set limit {} to {}", limitName, maxValue);
    }

    public PolicyLimit getLimit(String limitName) {
        return limits.get(limitName);
    }

    public Map<String, PolicyLimit> getAllLimits() {
        return Map.copyOf(limits);
    }

    public void resetUsage(String limitName) {
        AtomicInteger counter = usageCounters.get(limitName);
        if (counter != null) {
            counter.set(0);
        }
    }

    public void resetAllUsage() {
        for (AtomicInteger counter : usageCounters.values()) {
            counter.set(0);
        }
        log.info("Reset all limit usage counters");
    }

    public record PolicyLimit(
        String name,
        long maxValue,
        long currentValue,
        LimitType type
    ) {}

    public enum LimitType {
        TOKEN,
        RATE,
        CONCURRENT,
        SIZE,
        CONTEXT,
        GENERAL
    }
}