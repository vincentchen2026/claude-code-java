package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InputReader command parsing.
 */
class InputReaderTest {

    @Test
    void parseCommandSimple() {
        InputReader.ParsedCommand cmd = InputReader.parseCommand("/help");
        assertNotNull(cmd);
        assertEquals("help", cmd.name());
        assertEquals("", cmd.arguments());
    }

    @Test
    void parseCommandWithArguments() {
        InputReader.ParsedCommand cmd = InputReader.parseCommand("/model claude-3");
        assertNotNull(cmd);
        assertEquals("model", cmd.name());
        assertEquals("claude-3", cmd.arguments());
    }

    @Test
    void parseCommandWithMultipleArguments() {
        InputReader.ParsedCommand cmd = InputReader.parseCommand("/config set key value");
        assertNotNull(cmd);
        assertEquals("config", cmd.name());
        assertEquals("set key value", cmd.arguments());
    }

    @Test
    void parseCommandNullInput() {
        assertNull(InputReader.parseCommand(null));
    }

    @Test
    void parseCommandNonSlashInput() {
        assertNull(InputReader.parseCommand("hello world"));
    }

    @Test
    void parseCommandEmptyInput() {
        assertNull(InputReader.parseCommand(""));
    }

    @Test
    void parseCommandWithLeadingSpaces() {
        InputReader.ParsedCommand cmd = InputReader.parseCommand("/exit  ");
        assertNotNull(cmd);
        assertEquals("exit", cmd.name());
    }

    @Test
    void parseCommandWithExtraSpaces() {
        InputReader.ParsedCommand cmd = InputReader.parseCommand("/compact   some args");
        assertNotNull(cmd);
        assertEquals("compact", cmd.name());
        assertEquals("some args", cmd.arguments());
    }
}
