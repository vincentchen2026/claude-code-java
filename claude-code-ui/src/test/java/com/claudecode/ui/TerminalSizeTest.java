package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TerminalSize record.
 */
class TerminalSizeTest {

    @Test
    void defaultSizeIs80x24() {
        assertEquals(80, TerminalSize.DEFAULT.columns());
        assertEquals(24, TerminalSize.DEFAULT.rows());
    }

    @Test
    void customSize() {
        TerminalSize size = new TerminalSize(120, 40);
        assertEquals(120, size.columns());
        assertEquals(40, size.rows());
    }

    @Test
    void equalityWorks() {
        TerminalSize a = new TerminalSize(80, 24);
        TerminalSize b = new TerminalSize(80, 24);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityWorks() {
        TerminalSize a = new TerminalSize(80, 24);
        TerminalSize b = new TerminalSize(120, 40);
        assertNotEquals(a, b);
    }

    @Test
    void toStringContainsDimensions() {
        TerminalSize size = new TerminalSize(100, 50);
        String str = size.toString();
        assertTrue(str.contains("100"));
        assertTrue(str.contains("50"));
    }
}
