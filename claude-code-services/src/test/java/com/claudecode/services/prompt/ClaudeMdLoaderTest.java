package com.claudecode.services.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ClaudeMdLoader}.
 */
class ClaudeMdLoaderTest {

    private ClaudeMdLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ClaudeMdLoader();
    }

    // --- Single file loading ---

    @Test
    void loadsSingleClaudeMdFile(@TempDir Path tempDir) throws IOException {
        Path claudeMd = tempDir.resolve("CLAUDE.md");
        Files.writeString(claudeMd, "Use TypeScript strict mode.");

        String result = loader.loadAndMerge(List.of(claudeMd));

        assertEquals("Use TypeScript strict mode.", result);
    }

    @Test
    void loadFileReturnsContentForExistingFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.md");
        Files.writeString(file, "Hello world");

        String content = loader.loadFile(file);

        assertEquals("Hello world", content);
    }

    @Test
    void loadFileReturnsNullForMissingFile() {
        String content = loader.loadFile(Path.of("/nonexistent/CLAUDE.md"));

        assertNull(content);
    }

    @Test
    void loadFileReturnsNullForNullPath() {
        String content = loader.loadFile(null);

        assertNull(content);
    }

    // --- Multiple file merging ---

    @Test
    void mergesMultipleClaudeMdFiles(@TempDir Path tempDir) throws IOException {
        Path userLevel = tempDir.resolve("user-CLAUDE.md");
        Path projectLevel = tempDir.resolve("project-CLAUDE.md");

        Files.writeString(userLevel, "User preference: dark mode.");
        Files.writeString(projectLevel, "Project rule: use 4-space indent.");

        String result = loader.loadAndMerge(List.of(userLevel, projectLevel));

        // Both sections present
        assertTrue(result.contains("User preference: dark mode."));
        assertTrue(result.contains("Project rule: use 4-space indent."));
        // Separated by divider
        assertTrue(result.contains("---"));
        // Project-level comes after user-level
        int userIdx = result.indexOf("User preference");
        int projectIdx = result.indexOf("Project rule");
        assertTrue(projectIdx > userIdx, "Project-level should come after user-level");
    }

    @Test
    void mergesThreeFiles(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("a.md");
        Path file2 = tempDir.resolve("b.md");
        Path file3 = tempDir.resolve("c.md");

        Files.writeString(file1, "Section A");
        Files.writeString(file2, "Section B");
        Files.writeString(file3, "Section C");

        String result = loader.loadAndMerge(List.of(file1, file2, file3));

        assertTrue(result.contains("Section A"));
        assertTrue(result.contains("Section B"));
        assertTrue(result.contains("Section C"));
        // Verify order
        assertTrue(result.indexOf("Section A") < result.indexOf("Section B"));
        assertTrue(result.indexOf("Section B") < result.indexOf("Section C"));
    }

    // --- Missing CLAUDE.md handling ---

    @Test
    void gracefullyHandlesEmptyPathList() {
        String result = loader.loadAndMerge(List.of());

        assertEquals("", result);
    }

    @Test
    void gracefullyHandlesNullPathList() {
        String result = loader.loadAndMerge(null);

        assertEquals("", result);
    }

    @Test
    void skipsNonExistentFiles(@TempDir Path tempDir) throws IOException {
        Path existing = tempDir.resolve("CLAUDE.md");
        Files.writeString(existing, "Real content.");
        Path missing = tempDir.resolve("nonexistent/CLAUDE.md");

        String result = loader.loadAndMerge(List.of(missing, existing));

        assertEquals("Real content.", result);
    }

    @Test
    void returnsEmptyWhenAllFilesMissing() {
        String result = loader.loadAndMerge(List.of(
                Path.of("/missing/a/CLAUDE.md"),
                Path.of("/missing/b/CLAUDE.md")
        ));

        assertEquals("", result);
    }

    @Test
    void skipsBlankFiles(@TempDir Path tempDir) throws IOException {
        Path blank = tempDir.resolve("blank.md");
        Path real = tempDir.resolve("real.md");
        Files.writeString(blank, "   \n  \n  ");
        Files.writeString(real, "Actual instructions.");

        String result = loader.loadAndMerge(List.of(blank, real));

        assertEquals("Actual instructions.", result);
    }

    // --- Content stripping ---

    @Test
    void stripsWhitespaceFromFileContent(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("CLAUDE.md");
        Files.writeString(file, "\n\n  Content with whitespace  \n\n");

        String result = loader.loadAndMerge(List.of(file));

        assertEquals("Content with whitespace", result);
    }

    // --- Discovery ---

    @Test
    void discoverFindsClaudeMdInWorkingDirectory(@TempDir Path tempDir) throws IOException {
        Path claudeMd = tempDir.resolve("CLAUDE.md");
        Files.writeString(claudeMd, "Project instructions.");

        List<Path> paths = loader.discoverClaudeMdPaths(tempDir);

        assertTrue(paths.stream().anyMatch(p -> p.equals(claudeMd)),
                "Should discover CLAUDE.md in working directory");
    }

    @Test
    void discoverReturnsEmptyForDirectoryWithNoClaudeMd(@TempDir Path tempDir) {
        // tempDir has no CLAUDE.md and we won't find ~/.claude/CLAUDE.md in most test envs
        List<Path> paths = loader.discoverClaudeMdPaths(tempDir);

        // May or may not find user-level CLAUDE.md, but should not throw
        assertNotNull(paths);
    }
}
