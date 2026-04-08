package com.claudecode.services.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExtractMemoriesService {

    private static final Logger log = LoggerFactory.getLogger(ExtractMemoriesService.class);

    private final Map<String, List<Memory>> memories = new ConcurrentHashMap<>();
    private final LlmClient llmClient;

    public ExtractMemoriesService() {
        this.llmClient = null;
    }

    public ExtractMemoriesService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public List<Memory> extractMemories(String sessionId, List<ConversationTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return List.of();
        }

        List<Memory> extracted = new ArrayList<>();

        List<String> importantTopics = extractTopics(turns);
        for (String topic : importantTopics) {
            Memory memory = new Memory(
                "mem_" + System.currentTimeMillis(),
                topic,
                MemoryType.TOPIC,
                0.8,
                Instant.now(),
                sessionId
            );
            extracted.add(memory);
        }

        List<String> keyDecisions = extractDecisions(turns);
        for (String decision : keyDecisions) {
            Memory memory = new Memory(
                "mem_" + System.currentTimeMillis(),
                decision,
                MemoryType.DECISION,
                0.9,
                Instant.now(),
                sessionId
            );
            extracted.add(memory);
        }

        memories.put(sessionId, extracted);
        log.info("Extracted {} memories from session {}", extracted.size(), sessionId);

        return extracted;
    }

    public void addMemory(String sessionId, Memory memory) {
        List<Memory> sessionMemories = memories.computeIfAbsent(sessionId, k -> new ArrayList<>());
        sessionMemories.add(memory);
    }

    public List<Memory> getMemories(String sessionId) {
        return memories.getOrDefault(sessionId, List.of());
    }

    public List<Memory> getMemoriesByType(String sessionId, MemoryType type) {
        return memories.getOrDefault(sessionId, List.of()).stream()
            .filter(m -> m.type() == type)
            .toList();
    }

    public List<Memory> getRecentMemories(String sessionId, int limit) {
        return memories.getOrDefault(sessionId, List.of()).stream()
            .sorted(Comparator.comparing(Memory::timestamp).reversed())
            .limit(limit)
            .toList();
    }

    public void clearMemories(String sessionId) {
        memories.remove(sessionId);
    }

    private List<String> extractTopics(List<ConversationTurn> turns) {
        List<String> topics = new ArrayList<>();

        for (ConversationTurn turn : turns) {
            String[] words = turn.content().split("\\s+");
            for (String word : words) {
                if (word.length() > 6 && !isCommonWord(word)) {
                    topics.add(word);
                }
            }
        }

        return topics.stream().distinct().limit(10).toList();
    }

    private List<String> extractDecisions(List<ConversationTurn> turns) {
        List<String> decisions = new ArrayList<>();

        for (ConversationTurn turn : turns) {
            if (turn.content().toLowerCase().contains("decided") ||
                turn.content().toLowerCase().contains("chose") ||
                turn.content().toLowerCase().contains("selected")) {
                decisions.add(turn.content().substring(0, Math.min(100, turn.content().length())));
            }
        }

        return decisions;
    }

    private boolean isCommonWord(String word) {
        List<String> common = List.of("the", "be", "have", "that", "this", "with", "from", "they", "will", "would");
        return common.contains(word.toLowerCase());
    }

    public record Memory(
        String id,
        String content,
        MemoryType type,
        double confidence,
        Instant timestamp,
        String sessionId
    ) {}

    public record ConversationTurn(
        String role,
        String content,
        Instant timestamp
    ) {}

    public enum MemoryType {
        TOPIC,
        DECISION,
        PREFERENCE,
        FACT,
        SUGGESTION
    }

    public interface LlmClient {
        String summarize(String text);
    }
}