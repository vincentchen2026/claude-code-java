package com.claudecode.services.ultrareview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UltrareviewQuotaService {

    private static final Logger log = LoggerFactory.getLogger(UltrareviewQuotaService.class);

    private final Map<String, QuotaEntry> quotas = new ConcurrentHashMap<>();
    private final int defaultMonthlyQuota;

    public UltrareviewQuotaService() {
        this(10);
    }

    public UltrareviewQuotaService(int defaultMonthlyQuota) {
        this.defaultMonthlyQuota = defaultMonthlyQuota;
    }

    public boolean hasQuota(String userId) {
        QuotaEntry entry = quotas.get(userId);
        if (entry == null) {
            return true;
        }

        if (entry.renewedAt().isBefore(entry.currentPeriodStart())) {
            return true;
        }

        return entry.usedCount() < entry.quota();
    }

    public void useQuota(String userId) {
        QuotaEntry entry = quotas.computeIfAbsent(userId, k -> createNewEntry(k));

        if (entry.renewedAt().isBefore(entry.currentPeriodStart())) {
            entry = renewEntry(entry);
        }

        AtomicInteger used = new AtomicInteger(entry.usedCount());
        quotas.put(userId, new QuotaEntry(
            entry.userId(),
            entry.quota(),
            used.incrementAndGet(),
            entry.currentPeriodStart(),
            entry.renewedAt()
        ));

        log.debug("User {} used quota, now at {}/{}", userId, used.get(), entry.quota());
    }

    public QuotaStatus getQuotaStatus(String userId) {
        QuotaEntry entry = quotas.get(userId);
        if (entry == null) {
            return new QuotaStatus(defaultMonthlyQuota, 0, defaultMonthlyQuota, true);
        }

        if (entry.renewedAt().isBefore(entry.currentPeriodStart())) {
            return new QuotaStatus(defaultMonthlyQuota, 0, defaultMonthlyQuota, true);
        }

        int remaining = Math.max(0, entry.quota() - entry.usedCount());
        return new QuotaStatus(entry.quota(), entry.usedCount(), remaining, remaining > 0);
    }

    public void setQuota(String userId, int quota) {
        QuotaEntry entry = quotas.get(userId);
        if (entry != null) {
            quotas.put(userId, new QuotaEntry(
                userId,
                quota,
                entry.usedCount(),
                entry.currentPeriodStart(),
                Instant.now()
            ));
        } else {
            quotas.put(userId, createQuotaEntry(userId, quota));
        }
        log.info("Set quota for user {} to {}", userId, quota);
    }

    private QuotaEntry createNewEntry(String userId) {
        return createQuotaEntry(userId, defaultMonthlyQuota);
    }

    private QuotaEntry createQuotaEntry(String userId, int quota) {
        return new QuotaEntry(
            userId,
            quota,
            0,
            Instant.now(),
            Instant.now()
        );
    }

    private QuotaEntry renewEntry(QuotaEntry entry) {
        return new QuotaEntry(
            entry.userId(),
            entry.quota(),
            0,
            Instant.now(),
            Instant.now()
        );
    }

    public record QuotaEntry(
        String userId,
        int quota,
        int usedCount,
        Instant currentPeriodStart,
        Instant renewedAt
    ) {}

    public record QuotaStatus(
        int totalQuota,
        int used,
        int remaining,
        boolean hasQuota
    ) {}
}