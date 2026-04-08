package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ANSI style and color utilities.
 */
class AnsiStyleTest {

    @Test
    void styledAppliesBoldCodes() {
        String result = Ansi.styled("hello", AnsiStyle.BOLD);
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains("\u001B[1m"), "Should contain bold on code");
            assertTrue(result.contains("\u001B[0m"), "Should contain reset code");
            assertTrue(result.contains("hello"));
        } else {
            assertEquals("hello", result);
        }
    }

    @Test
    void styledAppliesMultipleStyles() {
        String result = Ansi.styled("text", AnsiStyle.BOLD, AnsiStyle.ITALIC);
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains("\u001B[1m"));
            assertTrue(result.contains("\u001B[3m"));
            assertTrue(result.contains("text"));
        } else {
            assertEquals("text", result);
        }
    }

    @Test
    void styledWithNoStylesReturnsPlainText() {
        String result = Ansi.styled("plain");
        assertEquals("plain", result);
    }

    @Test
    void coloredAppliesColorCode() {
        String result = Ansi.colored("red text", AnsiColor.RED);
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains("\u001B[31m"), "Should contain red color code");
            assertTrue(result.contains("red text"));
            assertTrue(result.contains("\u001B[0m"), "Should contain reset");
        } else {
            assertEquals("red text", result);
        }
    }

    @Test
    void styledWithColorAndStyles() {
        String result = Ansi.styled("fancy", AnsiColor.GREEN, AnsiStyle.BOLD, AnsiStyle.UNDERLINE);
        if (Ansi.isColorSupported()) {
            assertTrue(result.contains("\u001B[32m"), "Should contain green code");
            assertTrue(result.contains("\u001B[1m"), "Should contain bold code");
            assertTrue(result.contains("\u001B[4m"), "Should contain underline code");
            assertTrue(result.contains("fancy"));
        } else {
            assertEquals("fancy", result);
        }
    }

    @Test
    void allAnsiStylesHaveOnAndOffCodes() {
        for (AnsiStyle style : AnsiStyle.values()) {
            assertNotNull(style.on(), style.name() + " should have on code");
            assertNotNull(style.off(), style.name() + " should have off code");
            assertTrue(style.on().startsWith("\u001B["), style.name() + " on code should be ANSI escape");
            assertTrue(style.off().startsWith("\u001B["), style.name() + " off code should be ANSI escape");
        }
    }

    @Test
    void allAnsiColorsHaveCodes() {
        for (AnsiColor color : AnsiColor.values()) {
            assertNotNull(color.code(), color.name() + " should have color code");
            assertTrue(color.code().startsWith("\u001B["), color.name() + " code should be ANSI escape");
        }
    }

    @Test
    void ansiColorEnumHasExpectedValues() {
        assertEquals(8, AnsiColor.values().length);
        assertNotNull(AnsiColor.RED);
        assertNotNull(AnsiColor.GREEN);
        assertNotNull(AnsiColor.YELLOW);
        assertNotNull(AnsiColor.BLUE);
        assertNotNull(AnsiColor.MAGENTA);
        assertNotNull(AnsiColor.CYAN);
        assertNotNull(AnsiColor.WHITE);
        assertNotNull(AnsiColor.GRAY);
    }

    @Test
    void ansiStyleEnumHasExpectedValues() {
        assertEquals(5, AnsiStyle.values().length);
        assertNotNull(AnsiStyle.BOLD);
        assertNotNull(AnsiStyle.DIM);
        assertNotNull(AnsiStyle.ITALIC);
        assertNotNull(AnsiStyle.UNDERLINE);
        assertNotNull(AnsiStyle.STRIKETHROUGH);
    }
}
