package com.claudecode.services.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsSyncTest {

    @TempDir
    Path tempDir;

    // --- StubSettingsSyncService ---

    @Test
    void stubSyncServiceDisabledByDefault() {
        var service = new StubSettingsSyncService();
        assertFalse(service.isEnabled());
    }

    @Test
    void stubSyncServicePushReturnsFalse() {
        var service = new StubSettingsSyncService();
        assertFalse(service.push(Map.of("key", "value")));
    }

    @Test
    void stubSyncServicePullReturnsEmpty() {
        var service = new StubSettingsSyncService();
        assertTrue(service.pull().isEmpty());
    }

    @Test
    void stubSyncServiceLastSyncEmpty() {
        var service = new StubSettingsSyncService();
        assertTrue(service.lastSyncTimestamp().isEmpty());
    }

    @Test
    void stubSyncServiceCanBeEnabled() {
        var service = new StubSettingsSyncService(true);
        assertTrue(service.isEnabled());
    }

    // --- StubRemoteManagedSettings ---

    @Test
    void stubRemoteManagedDisabledByDefault() {
        var managed = new StubRemoteManagedSettings();
        assertFalse(managed.isEnabled());
    }

    @Test
    void stubRemoteManagedFetchReturnsEmpty() {
        var managed = new StubRemoteManagedSettings();
        assertTrue(managed.fetchManagedSettings().isEmpty());
    }

    @Test
    void stubRemoteManagedNothingIsManaged() {
        var managed = new StubRemoteManagedSettings();
        assertFalse(managed.isManaged("any.key"));
        assertTrue(managed.getManagedValue("any.key").isEmpty());
    }

    @Test
    void stubRemoteManagedCanBeEnabled() {
        var managed = new StubRemoteManagedSettings(true);
        assertTrue(managed.isEnabled());
    }

    // --- SyncCache ---

    @Test
    void syncCacheWriteAndRead() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        cache.write("{\"settings\": true}");
        assertTrue(cache.exists());
        assertEquals("{\"settings\": true}", cache.read().orElse(""));
    }

    @Test
    void syncCacheReadWhenEmpty() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        assertFalse(cache.exists());
        assertTrue(cache.read().isEmpty());
    }

    @Test
    void syncCacheLastWriteTime() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        assertTrue(cache.lastWriteTime().isEmpty());

        cache.write("data");
        var ts = cache.lastWriteTime();
        assertTrue(ts.isPresent());
        // Should be recent (within last 5 seconds)
        assertTrue(Instant.now().getEpochSecond() - ts.get().getEpochSecond() < 5);
    }

    @Test
    void syncCacheIsStaleWhenEmpty() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        assertTrue(cache.isStale(3600));
    }

    @Test
    void syncCacheIsNotStaleWhenFresh() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        cache.write("data");
        assertFalse(cache.isStale(3600)); // 1 hour — should not be stale
    }

    @Test
    void syncCacheClear() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        cache.write("data");
        assertTrue(cache.exists());
        cache.clear();
        assertFalse(cache.exists());
        assertTrue(cache.lastWriteTime().isEmpty());
    }

    @Test
    void syncCacheOverwrite() {
        var cache = new SyncCache(tempDir.resolve("cache"));
        cache.write("first");
        cache.write("second");
        assertEquals("second", cache.read().orElse(""));
    }

    @Test
    void syncCacheGetCacheDir() {
        Path dir = tempDir.resolve("myCache");
        var cache = new SyncCache(dir);
        assertEquals(dir, cache.getCacheDir());
    }
}
