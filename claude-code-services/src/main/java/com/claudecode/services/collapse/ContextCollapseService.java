package com.claudecode.services.collapse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ContextCollapseService {

    private static final Logger log = LoggerFactory.getLogger(ContextCollapseService.class);

    private final Map<String, ContextIndex> indices = new ConcurrentHashMap<>();
    private final Path persistDir;
    private final int maxContextItems;

    public ContextCollapseService() {
        this(Path.of(System.getProperty("user.home"), ".claude", "collapse"));
    }

    public ContextCollapseService(Path persistDir) {
        this.persistDir = persistDir;
        this.maxContextItems = 100;
    }

    public String createIndex(String sessionId) {
        ContextIndex index = new ContextIndex(
            sessionId,
            new ArrayList<>(),
            Instant.now(),
            0
        );
        indices.put(sessionId, index);
        log.debug("Created context index for session: {}", sessionId);
        return sessionId;
    }

    public void addContextItem(String sessionId, ContextItem item) {
        ContextIndex index = indices.computeIfAbsent(sessionId, this::createIndexIndex);
        index.items().add(item);
        index = new ContextIndex(
            index.sessionId(),
            index.items(),
            index.createdAt(),
            index.collapseCount() + 1
        );
        indices.put(sessionId, index);
    }

    public List<ContextItem> getContextItems(String sessionId, int limit) {
        ContextIndex index = indices.get(sessionId);
        if (index == null) {
            return List.of();
        }
        return index.items().stream()
            .sorted(Comparator.comparing(ContextItem::importance).reversed())
            .limit(limit)
            .toList();
    }

    public CollapseResult collapseContext(String sessionId, CollapseStrategy strategy) {
        ContextIndex index = indices.get(sessionId);
        if (index == null) {
            return new CollapseResult(0, 0, List.of());
        }

        List<ContextItem> items = index.items();
        int originalCount = items.size();

        List<ContextItem> collapsed = switch (strategy) {
            case IMPORTANCE -> collapseByImportance(items);
            case RECENCY -> collapseByRecency(items);
            case HYBRID -> collapseByHybrid(items);
        };

        int removedCount = originalCount - collapsed.size();

        ContextIndex updated = new ContextIndex(
            sessionId,
            collapsed,
            index.createdAt(),
            index.collapseCount() + 1
        );
        indices.put(sessionId, updated);

        log.info("Collapsed context for session {}: {} -> {} items (removed {})",
            sessionId, originalCount, collapsed.size(), removedCount);

        return new CollapseResult(originalCount, removedCount, collapsed);
    }

    private List<ContextItem> collapseByImportance(List<ContextItem> items) {
        int targetSize = Math.max(10, items.size() / 2);
        return items.stream()
            .sorted(Comparator.comparing(ContextItem::importance).reversed())
            .limit(targetSize)
            .collect(Collectors.toList());
    }

    private List<ContextItem> collapseByRecency(List<ContextItem> items) {
        int targetSize = Math.max(10, items.size() / 2);
        return items.stream()
            .sorted(Comparator.comparing(ContextItem::timestamp).reversed())
            .limit(targetSize)
            .collect(Collectors.toList());
    }

    private List<ContextItem> collapseByHybrid(List<ContextItem> items) {
        int targetSize = Math.max(10, items.size() / 2);

        double avgImportance = items.stream()
            .mapToDouble(ContextItem::importance)
            .average()
            .orElse(0.5);

        Instant cutoff = Instant.now().minusSeconds(3600);

        return items.stream()
            .filter(item -> item.importance() >= avgImportance || item.timestamp().isAfter(cutoff))
            .sorted(Comparator.comparing(ContextItem::importance).reversed())
            .limit(targetSize)
            .collect(Collectors.toList());
    }

    public void persist(String sessionId) {
        ContextIndex index = indices.get(sessionId);
        if (index == null) {
            return;
        }
        log.debug("Persisting context index for session: {}", sessionId);
    }

    public ContextIndex load(String sessionId) {
        ContextIndex index = indices.get(sessionId);
        if (index != null) {
            log.debug("Loaded context index for session: {}", sessionId);
        }
        return index;
    }

    public void clear(String sessionId) {
        indices.remove(sessionId);
        log.info("Cleared context index for session: {}", sessionId);
    }

    private ContextIndex createIndexIndex(String sessionId) {
        return new ContextIndex(sessionId, new ArrayList<>(), Instant.now(), 0);
    }

    public record ContextIndex(
        String sessionId,
        List<ContextItem> items,
        Instant createdAt,
        int collapseCount
    ) {}

    public record ContextItem(
        String id,
        String content,
        ContextType type,
        double importance,
        Instant timestamp,
        Map<String, String> metadata
    ) {}

    public record CollapseResult(
        int originalCount,
        int removedCount,
        List<ContextItem> remainingItems
    ) {}

    public enum CollapseStrategy {
        IMPORTANCE,
        RECENCY,
        HYBRID
    }

    public enum ContextType {
        USER_MESSAGE,
        ASSISTANT_MESSAGE,
        TOOL_CALL,
        TOOL_RESULT,
        SYSTEM_PROMPT,
        MEMORY
    }
}