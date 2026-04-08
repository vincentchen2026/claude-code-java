package com.claudecode.services.compact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * CompactCacheConfig — Cached/Time-based MC configuration (P2 stub).
 * Manages TTL-based caching for compaction results.
 */
public class CompactCacheConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CompactCacheConfig.class);

    private Duration cacheTtl = Duration.ofMinutes(5);
    private int maxCacheEntries = 100;
    private boolean enabled = false;

    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration ttl) { this.cacheTtl = ttl; }

    public int getMaxCacheEntries() { return maxCacheEntries; }
    public void setMaxCacheEntries(int max) { this.maxCacheEntries = max; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Get a cached compaction result (stub). */
    public String getCached(String key) {
        LOG.debug("CompactCacheConfig.getCached: Not yet implemented");
        return null;
    }

    /** Store a compaction result in cache (stub). */
    public void putCached(String key, String value) {
        LOG.debug("CompactCacheConfig.putCached: Not yet implemented");
    }
}
