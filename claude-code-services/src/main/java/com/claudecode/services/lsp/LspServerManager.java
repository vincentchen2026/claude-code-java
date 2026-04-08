package com.claudecode.services.lsp;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

public class LspServerManager {

    private final Map<String, ServerEntry> servers;
    private final ExecutorService executor;
    private final Map<String, Instant> lastUsed;

    public LspServerManager() {
        this.servers = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool();
        this.lastUsed = new ConcurrentHashMap<>();
    }

    public void registerServer(String id, LspServerInstance instance) {
        servers.put(id, new ServerEntry(id, instance, Instant.now()));
    }

    public LspServerInstance getServer(String id) {
        ServerEntry entry = servers.get(id);
        if (entry != null) {
            lastUsed.put(id, Instant.now());
            LspServerInstance inst = entry.instance();
            return inst;
        }
        return null;
    }

    public void unregisterServer(String id) {
        ServerEntry entry = servers.remove(id);
        if (entry != null && entry.instance().isRunning()) {
            entry.instance().shutdown();
        }
    }

    public boolean isRunning(String id) {
        ServerEntry entry = servers.get(id);
        return entry != null && entry.instance().isRunning();
    }

    public java.util.List<String> getRegisteredServerIds() {
        return servers.values().stream()
            .map(ServerEntry::id)
            .toList();
    }

    public java.util.List<String> getRunningServerIds() {
        return servers.values().stream()
            .filter(e -> e.instance().isRunning())
            .map(ServerEntry::id)
            .toList();
    }

    public void cleanupIdleServers(long idleTimeoutMinutes) {
        Instant cutoff = Instant.now().minusSeconds(idleTimeoutMinutes * 60);
        servers.entrySet().removeIf(entry -> {
            Instant last = lastUsed.get(entry.getKey());
            if (last != null && last.isBefore(cutoff) && entry.getValue().instance().isRunning()) {
                entry.getValue().instance().shutdown();
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        for (ServerEntry entry : servers.values()) {
            if (entry.instance().isRunning()) {
                entry.instance().shutdown();
            }
        }
        servers.clear();
        executor.shutdown();
    }

    private record ServerEntry(
        String id,
        LspServerInstance instance,
        Instant registeredAt
    ) {}
}