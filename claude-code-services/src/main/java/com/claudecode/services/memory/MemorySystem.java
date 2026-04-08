package com.claudecode.services.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core memory system managing MEMORY.md as the entry point.
 * Enforces a 200 line / 25KB cap on the index file.
 * Uses two-step write: write to independent file + update MEMORY.md index.
 */
public class MemorySystem {

    private static final Logger LOG = LoggerFactory.getLogger(MemorySystem.class);

    static final int MAX_LINES = 200;
    static final int MAX_BYTES = 25 * 1024; // 25KB
    static final String MEMORY_FILE = "MEMORY.md";
    static final String MEMORIES_DIR = ".claude/memories";

    private final Path workingDirectory;
    private final List<MemoryEntry> entries;

    public MemorySystem(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.entries = new ArrayList<>();
    }

    /**
     * Add a memory entry using two-step write:
     * 1. Write content to independent file in .claude/memories/
     * 2. Update MEMORY.md index
     *
     * @param entry the memory entry to add
     * @throws IOException if writing fails
     */
    public void addMemory(MemoryEntry entry) throws IOException {
        // Step 1: Write to independent file
        Path memoriesDir = workingDirectory.resolve(MEMORIES_DIR);
        Files.createDirectories(memoriesDir);

        String fileName = entry.id() + ".md";
        Path memoryFile = memoriesDir.resolve(fileName);
        Files.writeString(memoryFile, formatMemoryFile(entry));

        // Step 2: Update index
        entries.add(entry);
        updateIndex();
    }

    /**
     * Update the MEMORY.md index file, enforcing size caps.
     */
    void updateIndex() throws IOException {
        String indexContent = buildIndexContent();

        // Enforce caps
        String[] lines = indexContent.split("\n");
        if (lines.length > MAX_LINES) {
            indexContent = Arrays.stream(lines)
                    .limit(MAX_LINES)
                    .collect(Collectors.joining("\n"));
            indexContent += "\n\n<!-- Truncated: exceeded " + MAX_LINES + " line limit -->";
        }

        if (indexContent.getBytes().length > MAX_BYTES) {
            // Truncate to fit within byte limit
            while (indexContent.getBytes().length > MAX_BYTES && !entries.isEmpty()) {
                entries.remove(0); // Remove oldest
                indexContent = buildIndexContent();
            }
        }

        Path indexFile = workingDirectory.resolve(MEMORY_FILE);
        Files.writeString(indexFile, indexContent);
    }

    /**
     * Build the MEMORY.md index content from current entries.
     */
    String buildIndexContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Memory\n\n");

        Map<MemoryEntry.MemoryCategory, List<MemoryEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(MemoryEntry::category, LinkedHashMap::new, Collectors.toList()));

        for (var group : grouped.entrySet()) {
            sb.append("## ").append(group.getKey().name().toLowerCase()).append("\n\n");
            for (MemoryEntry entry : group.getValue()) {
                sb.append("- ").append(entry.content());
                if (entry.source() != null) {
                    sb.append(" (source: ").append(entry.source()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Build a memory prompt for inclusion in the system prompt.
     *
     * @return formatted memory context string
     */
    public String buildMemoryPrompt() {
        if (entries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Relevant Memories\n\n");

        for (MemoryEntry entry : entries) {
            sb.append("- [").append(entry.category().name().toLowerCase()).append("] ");
            sb.append(entry.content()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Find memories relevant to a query using simple keyword matching.
     *
     * @param query the search query
     * @return list of matching memory entries
     */
    public List<MemoryEntry> findRelevantMemories(String query) {
        if (query == null || query.isBlank()) {
            return List.copyOf(entries);
        }

        String[] keywords = query.toLowerCase().split("\\s+");

        return entries.stream()
                .filter(entry -> {
                    String text = (entry.content() + " " +
                            (entry.source() != null ? entry.source() : "")).toLowerCase();
                    for (String keyword : keywords) {
                        if (text.contains(keyword)) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    /**
     * Load existing memories from MEMORY.md and .claude/memories/ directory.
     */
    public void loadExisting() throws IOException {
        Path memoriesDir = workingDirectory.resolve(MEMORIES_DIR);
        if (!Files.isDirectory(memoriesDir)) {
            return;
        }

        try (var files = Files.list(memoriesDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                 .sorted()
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         String id = file.getFileName().toString()
                                 .replace(".md", "");
                         // Simple parse: first line is category, rest is content
                         String[] parts = content.split("\n", 3);
                         MemoryEntry.MemoryCategory category = MemoryEntry.MemoryCategory.PROJECT;
                         String body = content;
                         if (parts.length >= 2 && parts[0].startsWith("category:")) {
                             try {
                                 category = MemoryEntry.MemoryCategory.valueOf(
                                         parts[0].substring(9).trim().toUpperCase());
                             } catch (IllegalArgumentException ignored) {
                             }
                             body = parts.length > 2 ? parts[2] : parts[1];
                         }
                         entries.add(MemoryEntry.of(id, category, body.trim(), file.toString()));
                     } catch (IOException e) {
                         LOG.warn("Failed to load memory file: {}", file, e);
                     }
                 });
        }
    }

    /**
     * Get all current memory entries.
     */
    public List<MemoryEntry> getEntries() {
        return List.copyOf(entries);
    }

    /**
     * Get the number of entries.
     */
    public int size() {
        return entries.size();
    }

    private String formatMemoryFile(MemoryEntry entry) {
        return "category: " + entry.category().name().toLowerCase() + "\n"
                + "created: " + entry.createdAt() + "\n"
                + entry.content() + "\n";
    }
}
