package com.claudecode.services.skills;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class SkillPrefetch {

    private final RemoteSkillLoader remoteLoader;
    private final Map<String, CompletableFuture<RemoteSkillLoader.Skill>> prefetchCache;
    private final ScheduledExecutorService scheduler;

    public SkillPrefetch(RemoteSkillLoader remoteLoader) {
        this.remoteLoader = remoteLoader;
        this.prefetchCache = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void prefetch(String skillId, String registryUrl) {
        prefetchCache.computeIfAbsent(skillId, id -> 
            remoteLoader.loadFromRegistry(id, registryUrl)
        );
    }

    public void prefetch(List<String> skillIds, String registryUrl) {
        for (String skillId : skillIds) {
            prefetch(skillId, registryUrl);
        }
    }

    public CompletableFuture<RemoteSkillLoader.Skill> getPrefetched(String skillId) {
        return prefetchCache.getOrDefault(skillId, CompletableFuture.failedFuture(
            new SkillPrefetchException("Skill not prefetched: " + skillId)
        ));
    }

    public boolean isPrefetched(String skillId) {
        CompletableFuture<RemoteSkillLoader.Skill> future = prefetchCache.get(skillId);
        return future != null && future.isDone() && !future.isCompletedExceptionally();
    }

    public void cancelPrefetch(String skillId) {
        CompletableFuture<RemoteSkillLoader.Skill> future = prefetchCache.remove(skillId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    public void clearPrefetchCache() {
        prefetchCache.clear();
    }

    public Map<String, CompletableFuture<RemoteSkillLoader.Skill>> getPendingPrefetches() {
        return Map.copyOf(prefetchCache);
    }

    public void schedulePrefetch(String skillId, String registryUrl, long delayMs) {
        scheduler.schedule(() -> prefetch(skillId, registryUrl), delayMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        clearPrefetchCache();
    }

    public static class SkillPrefetchException extends RuntimeException {
        public SkillPrefetchException(String message) {
            super(message);
        }
    }
}