package com.claudecode.services.hooks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HookMatcherTest {

    @Test
    void emptyMatcherMatchesEverything() {
        HookMatcher matcher = new HookMatcher(Optional.empty(), List.of());
        assertTrue(matcher.matches("Bash"));
        assertTrue(matcher.matches("FileRead"));
        assertTrue(matcher.matches(""));
    }

    @Test
    void blankMatcherMatchesEverything() {
        HookMatcher matcher = new HookMatcher(Optional.of(""), List.of());
        assertTrue(matcher.matches("Bash"));
    }

    @Test
    void exactMatchWorks() {
        HookMatcher matcher = new HookMatcher(Optional.of("Bash"), List.of());
        assertTrue(matcher.matches("Bash"));
        assertFalse(matcher.matches("FileRead"));
        assertFalse(matcher.matches("bash"));
    }

    @Test
    void wildcardMatchWorks() {
        HookMatcher matcher = new HookMatcher(Optional.of("File*"), List.of());
        assertTrue(matcher.matches("FileRead"));
        assertTrue(matcher.matches("FileWrite"));
        assertTrue(matcher.matches("FileEdit"));
        assertFalse(matcher.matches("Bash"));
    }

    @Test
    void wildcardMatchAll() {
        HookMatcher matcher = new HookMatcher(Optional.of("*"), List.of());
        assertTrue(matcher.matches("Bash"));
        assertTrue(matcher.matches("FileRead"));
        assertTrue(matcher.matches("anything"));
    }

    @Test
    void nullQueryDoesNotMatchExactPattern() {
        HookMatcher matcher = new HookMatcher(Optional.of("Bash"), List.of());
        assertFalse(matcher.matches(null));
    }
}
