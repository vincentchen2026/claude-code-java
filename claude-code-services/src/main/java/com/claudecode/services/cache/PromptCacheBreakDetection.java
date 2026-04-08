package com.claudecode.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PromptCacheBreakDetection {

    private static final Logger log = LoggerFactory.getLogger(PromptCacheBreakDetection.class);

    private final Map<String, CacheEntry> cacheEntries = new ConcurrentHashMap<>();
    private final Map<String, String> promptHashes = new ConcurrentHashMap<>();
    private final Duration maxCacheAge;

    public enum BreakReason {
        SYSTEM_PROMPT_CHANGED,
        CONFIG_CHANGED,
        TIME_EXPIRED,
        MANUAL_INVALIDATION,
        MODEL_CHANGED,
        CONTEXT_OVERFLOW
    }

    public PromptCacheBreakDetection() {
        this(Duration.ofHours(1));
    }

    public PromptCacheBreakDetection(Duration maxCacheAge) {
        this.maxCacheAge = maxCacheAge;
    }

    public String registerPrompt(String prompt, Map<String, String> context) {
        String hash = computeHash(prompt, context);
        CacheEntry entry = new CacheEntry(
            hash,
            prompt,
            context,
            Instant.now(),
            null,
            false
        );
        cacheEntries.put(hash, entry);
        promptHashes.put(hash, hash);
        log.debug("Registered prompt cache entry: {}", hash.substring(0, 8));
        return hash;
    }

    public CacheStatus checkCacheValidity(String prompt, Map<String, String> context) {
        String hash = computeHash(prompt, context);
        CacheEntry entry = cacheEntries.get(hash);

        if (entry == null) {
            return new CacheStatus(false, null, BreakReason.MANUAL_INVALIDATION);
        }

        if (entry.invalidated()) {
            return new CacheStatus(false, hash, BreakReason.MANUAL_INVALIDATION);
        }

        Instant ageLimit = Instant.now().minus(maxCacheAge.toMillis(), ChronoUnit.MILLIS);
        if (entry.createdAt().isBefore(ageLimit)) {
            return new CacheStatus(false, hash, BreakReason.TIME_EXPIRED);
        }

        return new CacheStatus(true, hash, null);
    }

    public void invalidateCache(String hash) {
        CacheEntry entry = cacheEntries.get(hash);
        if (entry != null) {
            CacheEntry updated = new CacheEntry(
                entry.hash(),
                entry.prompt(),
                entry.context(),
                entry.createdAt(),
                Instant.now(),
                true
            );
            cacheEntries.put(hash, updated);
            log.info("Invalidated cache entry: {}", hash.substring(0, 8));
        }
    }

    public void invalidateAll() {
        for (Map.Entry<String, CacheEntry> e : cacheEntries.entrySet()) {
            CacheEntry entry = e.getValue();
            CacheEntry updated = new CacheEntry(
                entry.hash(),
                entry.prompt(),
                entry.context(),
                entry.createdAt(),
                Instant.now(),
                true
            );
            cacheEntries.put(e.getKey(), updated);
        }
        log.info("Invalidated all cache entries");
    }

    public void invalidateIfContextChanged(String hash, Map<String, String> newContext) {
        CacheEntry entry = cacheEntries.get(hash);
        if (entry != null && !entry.context().equals(newContext)) {
            invalidateCache(hash);
            log.info("Invalidated cache due to context change: {}", hash.substring(0, 8));
        }
    }

    public boolean isCached(String hash) {
        CacheEntry entry = cacheEntries.get(hash);
        return entry != null && !entry.invalidated();
    }

    private String computeHash(String prompt, Map<String, String> context) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(prompt.getBytes());
            for (Map.Entry<String, String> e : context.entrySet()) {
                md.update(e.getKey().getBytes());
                md.update(e.getValue().getBytes());
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to compute hash", e);
            return String.valueOf(prompt.hashCode());
        }
    }

    public record CacheEntry(
        String hash,
        String prompt,
        Map<String, String> context,
        Instant createdAt,
        Instant invalidatedAt,
        boolean invalidated
    ) {}

    public record CacheStatus(
        boolean valid,
        String hash,
        BreakReason breakReason
    ) {}

    public static class Duration {
        private final long milliseconds;

        public Duration(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        public static Duration ofHours(long hours) {
            return new Duration(hours * 60 * 60 * 1000);
        }

        public static Duration ofMinutes(long minutes) {
            return new Duration(minutes * 60 * 1000);
        }

        public long toMillis() {
            return milliseconds;
        }
    }
}