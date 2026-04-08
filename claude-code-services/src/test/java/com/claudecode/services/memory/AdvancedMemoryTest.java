package com.claudecode.services.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedMemoryTest {

    @TempDir
    Path tempDir;

    @Test
    void kairosLogAddsAndRetrievesEntries() {
        var kairos = new KairosLog(tempDir);
        kairos.addEntry("test entry");

        List<String> entries = kairos.getEntries(LocalDate.now());
        assertFalse(entries.isEmpty());
        assertTrue(entries.get(0).contains("test entry"));
    }

    @Test
    void kairosLogReturnsEmptyForNoEntries() {
        var kairos = new KairosLog(tempDir);
        assertTrue(kairos.getEntries(LocalDate.of(2020, 1, 1)).isEmpty());
    }

    @Test
    void kairosLogTodaySummary() {
        var kairos = new KairosLog(tempDir);
        String summary = kairos.getTodaySummary();
        assertTrue(summary.contains("No KAIROS entries"));

        kairos.addEntry("did something");
        summary = kairos.getTodaySummary();
        assertTrue(summary.contains("did something"));
    }

    @Test
    void teamMemorySyncReturnsEmpty() {
        var sync = new TeamMemorySync();
        sync.sync();
        assertTrue(sync.getSharedMemories().isEmpty());
        assertTrue(sync.scanForSecrets("test content").isEmpty());
    }

    @Test
    void memoryAgeTrackerReturnsDefaults() {
        var tracker = new MemoryAgeTracker();
        assertNotNull(tracker.getAgeStats());
        tracker.recordAccess("test-id");
        assertNotNull(tracker.getLastAccess("test-id"));
    }

    @Test
    void autoDreamSystemLifecycle() {
        Path dreamsDir = tempDir.resolve("dreams");
        var kairos = new KairosLog(tempDir.resolve("kairos"));
        var dream = new AutoDreamSystem(kairos, dreamsDir);

        assertFalse(dream.isDreamLockActive());
        dream.startDream();
        assertTrue(dream.isDreamLockActive());
        dream.stopDream();
        assertFalse(dream.isDreamLockActive());
    }

    @Test
    void autoDreamSystemDistillsSkills() {
        Path kairosDir = tempDir.resolve("kairos");
        Path dreamsDir = tempDir.resolve("dreams");
        var kairos = new KairosLog(kairosDir);
        kairos.addEntry("learned about Java 21 features");
        kairos.addEntry("discovered virtual threads pattern");

        var dream = new AutoDreamSystem(kairos, dreamsDir);
        String result = dream.distillSkills();

        assertTrue(result.contains("Dream Consolidation"));
        assertTrue(result.contains("Java 21"));
        assertTrue(result.contains("virtual threads"));
    }

    @Test
    void autoDreamSystemNoEntries() {
        var kairos = new KairosLog(tempDir.resolve("kairos"));
        var dream = new AutoDreamSystem(kairos, tempDir.resolve("dreams"));

        String result = dream.distillSkills();
        assertTrue(result.contains("No KAIROS entries"));
    }
}
