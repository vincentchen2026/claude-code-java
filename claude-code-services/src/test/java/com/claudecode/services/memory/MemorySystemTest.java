package com.claudecode.services.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemorySystemTest {

    @TempDir
    Path tempDir;

    private MemorySystem memorySystem;

    @BeforeEach
    void setUp() {
        memorySystem = new MemorySystem(tempDir);
    }

    @Test
    void addMemoryCreatesFileAndUpdatesIndex() throws IOException {
        MemoryEntry entry = MemoryEntry.of("mem-1",
                MemoryEntry.MemoryCategory.PROJECT,
                "Project uses Java 21", "user");

        memorySystem.addMemory(entry);

        // Check independent file was created
        Path memFile = tempDir.resolve(".claude/memories/mem-1.md");
        assertTrue(Files.exists(memFile));

        // Check MEMORY.md index was updated
        Path indexFile = tempDir.resolve("MEMORY.md");
        assertTrue(Files.exists(indexFile));
        String indexContent = Files.readString(indexFile);
        assertTrue(indexContent.contains("Project uses Java 21"));
    }

    @Test
    void addMultipleMemories() throws IOException {
        memorySystem.addMemory(MemoryEntry.of("m1",
                MemoryEntry.MemoryCategory.USER, "User prefers dark mode", null));
        memorySystem.addMemory(MemoryEntry.of("m2",
                MemoryEntry.MemoryCategory.FEEDBACK, "Tests should be concise", null));

        assertEquals(2, memorySystem.size());
        assertEquals(2, memorySystem.getEntries().size());
    }

    @Test
    void buildMemoryPromptEmpty() {
        String prompt = memorySystem.buildMemoryPrompt();
        assertEquals("", prompt);
    }

    @Test
    void buildMemoryPromptWithEntries() throws IOException {
        memorySystem.addMemory(MemoryEntry.of("m1",
                MemoryEntry.MemoryCategory.PROJECT, "Uses Maven", null));
        memorySystem.addMemory(MemoryEntry.of("m2",
                MemoryEntry.MemoryCategory.USER, "Prefers verbose output", null));

        String prompt = memorySystem.buildMemoryPrompt();
        assertTrue(prompt.contains("Relevant Memories"));
        assertTrue(prompt.contains("Uses Maven"));
        assertTrue(prompt.contains("Prefers verbose output"));
    }

    @Test
    void findRelevantMemoriesWithKeyword() throws IOException {
        memorySystem.addMemory(MemoryEntry.of("m1",
                MemoryEntry.MemoryCategory.PROJECT, "Uses Maven for builds", null));
        memorySystem.addMemory(MemoryEntry.of("m2",
                MemoryEntry.MemoryCategory.PROJECT, "Uses Gradle for tests", null));

        List<MemoryEntry> results = memorySystem.findRelevantMemories("Maven");
        assertEquals(1, results.size());
        assertTrue(results.get(0).content().contains("Maven"));
    }

    @Test
    void findRelevantMemoriesEmptyQuery() throws IOException {
        memorySystem.addMemory(MemoryEntry.of("m1",
                MemoryEntry.MemoryCategory.PROJECT, "Entry 1", null));
        memorySystem.addMemory(MemoryEntry.of("m2",
                MemoryEntry.MemoryCategory.PROJECT, "Entry 2", null));

        List<MemoryEntry> results = memorySystem.findRelevantMemories("");
        assertEquals(2, results.size());
    }

    @Test
    void findRelevantMemoriesNoMatch() throws IOException {
        memorySystem.addMemory(MemoryEntry.of("m1",
                MemoryEntry.MemoryCategory.PROJECT, "Uses Java", null));

        List<MemoryEntry> results = memorySystem.findRelevantMemories("Python");
        assertTrue(results.isEmpty());
    }

    @Test
    void indexGroupsByCategory() throws IOException {
        memorySystem.addMemory(MemoryEntry.of("m1",
                MemoryEntry.MemoryCategory.USER, "User pref", null));
        memorySystem.addMemory(MemoryEntry.of("m2",
                MemoryEntry.MemoryCategory.PROJECT, "Project info", null));

        String indexContent = Files.readString(tempDir.resolve("MEMORY.md"));
        assertTrue(indexContent.contains("## user"));
        assertTrue(indexContent.contains("## project"));
    }
}
