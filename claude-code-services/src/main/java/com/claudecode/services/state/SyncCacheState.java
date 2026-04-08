package com.claudecode.services.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncCacheState {

    private final Path stateFile;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> entries;

    public SyncCacheState(Path stateDir) {
        this.stateFile = stateDir.resolve("sync-cache-state.json");
        this.objectMapper = new ObjectMapper();
        this.entries = new ConcurrentHashMap<>();
        load();
    }

    public void put(String key, String value) {
        entries.put(key, new CacheEntry(value, Instant.now().toString(), "string"));
        save();
    }

    public void put(String key, Object value, String type) {
        entries.put(key, new CacheEntry(value.toString(), Instant.now().toString(), type));
        save();
    }

    public String getString(String key) {
        CacheEntry entry = entries.get(key);
        return entry != null ? entry.value() : null;
    }

    public <T> T get(String key, Class<T> type) {
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        try {
            return objectMapper.readValue(entry.value(), type);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean contains(String key) {
        return entries.containsKey(key);
    }

    public void remove(String key) {
        entries.remove(key);
        save();
    }

    public void clear() {
        entries.clear();
        save();
    }

    public Map<String, CacheEntry> getAll() {
        return Map.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }

    public void load() {
        if (!Files.exists(stateFile)) {
            return;
        }
        try {
            String json = Files.readString(stateFile);
            SyncCacheStateData loaded = objectMapper.readValue(json, SyncCacheStateData.class);
            entries.clear();
            for (Map.Entry<String, CacheEntry> e : loaded.entries().entrySet()) {
                entries.put(e.getKey(), e.getValue());
            }
        } catch (IOException e) {
            // Start with empty state on error
        }
    }

    public void save() {
        try {
            Files.createDirectories(stateFile.getParent());
            SyncCacheStateData data = new SyncCacheStateData(entries);
            String json = objectMapper.writeValueAsString(data);
            Files.writeString(stateFile, json);
        } catch (IOException e) {
            // Log but don't fail
        }
    }

    public record CacheEntry(
        String value,
        String timestamp,
        String type
    ) {}

    private record SyncCacheStateData(
        Map<String, CacheEntry> entries
    ) {}
}