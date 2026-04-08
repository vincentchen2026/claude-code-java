package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for diff rendering with ANSI colors.
 */
class DiffRendererTest {

    @Test
    void renderAddedLineInGreen() {
        String result = DiffRenderer.renderDiffLine("+added line");
        assertTrue(result.contains("added line"));
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains(AnsiColor.GREEN.code()), "Added lines should be green");
        }
    }

    @Test
    void renderRemovedLineInRed() {
        String result = DiffRenderer.renderDiffLine("-removed line");
        assertTrue(result.contains("removed line"));
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains(AnsiColor.RED.code()), "Removed lines should be red");
        }
    }

    @Test
    void renderContextLineUnchanged() {
        String result = DiffRenderer.renderDiffLine(" context line");
        assertEquals(" context line", result);
    }

    @Test
    void renderHunkHeaderInCyan() {
        String result = DiffRenderer.renderDiffLine("@@ -1,3 +1,4 @@");
        assertTrue(result.contains("@@ -1,3 +1,4 @@"));
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains(AnsiColor.CYAN.code()), "Hunk headers should be cyan");
        }
    }

    @Test
    void renderFileHeaderBold() {
        String result = DiffRenderer.renderDiffLine("--- a/file.txt");
        assertTrue(result.contains("--- a/file.txt"));
    }

    @Test
    void renderEmptyLineReturnsEmpty() {
        assertEquals("", DiffRenderer.renderDiffLine(""));
        assertEquals("", DiffRenderer.renderDiffLine(null));
    }

    @Test
    void renderUnifiedDiffNull() {
        assertEquals("", DiffRenderer.renderUnifiedDiff(null));
        assertEquals("", DiffRenderer.renderUnifiedDiff(""));
    }

    @Test
    void renderUnifiedDiffColorsAllLines() {
        String diff = """
                --- a/test.txt
                +++ b/test.txt
                @@ -1,3 +1,3 @@
                 context
                -old line
                +new line""";
        String result = DiffRenderer.renderUnifiedDiff(diff);
        assertTrue(result.contains("context"));
        assertTrue(result.contains("old line"));
        assertTrue(result.contains("new line"));
    }

    @Test
    void generateDiffIdenticalTexts() {
        String text = "line1\nline2\nline3";
        String diff = DiffRenderer.generateDiff(text, text, "file.txt", 3);
        // Identical texts should produce header but no change hunks
        assertTrue(diff.contains("--- a/file.txt"));
        assertTrue(diff.contains("+++ b/file.txt"));
        assertFalse(diff.contains("@@"), "No hunks for identical content");
    }

    @Test
    void generateDiffWithAddedLine() {
        String oldText = "line1\nline2";
        String newText = "line1\nline2\nline3";
        String diff = DiffRenderer.generateDiff(oldText, newText, "test.txt", 3);
        assertTrue(diff.contains("+line3"), "Should show added line");
    }

    @Test
    void generateDiffWithRemovedLine() {
        String oldText = "line1\nline2\nline3";
        String newText = "line1\nline3";
        String diff = DiffRenderer.generateDiff(oldText, newText, "test.txt", 3);
        assertTrue(diff.contains("-line2"), "Should show removed line");
    }

    @Test
    void generateDiffWithModifiedLine() {
        String oldText = "line1\nold\nline3";
        String newText = "line1\nnew\nline3";
        String diff = DiffRenderer.generateDiff(oldText, newText, "test.txt", 3);
        assertTrue(diff.contains("-old"), "Should show removed old line");
        assertTrue(diff.contains("+new"), "Should show added new line");
    }

    @Test
    void generateAndRenderDiffProducesColoredOutput() {
        String oldText = "hello\nworld";
        String newText = "hello\nearth";
        String result = DiffRenderer.generateAndRenderDiff(oldText, newText, "greet.txt");
        assertTrue(result.contains("world"));
        assertTrue(result.contains("earth"));
    }

    @Test
    void computeDiffEmptyInputs() {
        List<DiffRenderer.DiffLine> result = DiffRenderer.computeDiff(new String[]{}, new String[]{});
        assertTrue(result.isEmpty());
    }

    @Test
    void computeDiffAddToEmpty() {
        List<DiffRenderer.DiffLine> result = DiffRenderer.computeDiff(
                new String[]{}, new String[]{"new"});
        assertEquals(1, result.size());
        assertEquals(DiffRenderer.DiffType.ADDED, result.get(0).type());
    }

    @Test
    void computeDiffRemoveAll() {
        List<DiffRenderer.DiffLine> result = DiffRenderer.computeDiff(
                new String[]{"old"}, new String[]{});
        assertEquals(1, result.size());
        assertEquals(DiffRenderer.DiffType.REMOVED, result.get(0).type());
    }
}
