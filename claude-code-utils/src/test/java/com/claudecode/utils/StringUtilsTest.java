package com.claudecode.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void isBlank_nullReturnsTrue() {
        assertTrue(StringUtils.isBlank(null));
    }

    @Test
    void isBlank_emptyReturnsTrue() {
        assertTrue(StringUtils.isBlank(""));
    }

    @Test
    void isBlank_whitespaceReturnsTrue() {
        assertTrue(StringUtils.isBlank("   "));
    }

    @Test
    void isBlank_nonBlankReturnsFalse() {
        assertFalse(StringUtils.isBlank("hello"));
    }

    @Test
    void isNotBlank_nonBlankReturnsTrue() {
        assertTrue(StringUtils.isNotBlank("hello"));
    }

    @Test
    void truncate_withinLimit() {
        assertEquals("hello", StringUtils.truncate("hello", 10));
    }

    @Test
    void truncate_exceedsLimit() {
        assertEquals("hel...", StringUtils.truncate("hello world", 6));
    }

    @Test
    void truncate_nullReturnsEmpty() {
        assertEquals("", StringUtils.truncate(null, 5));
    }

    @Test
    void truncate_shortMaxLength() {
        assertEquals("hel", StringUtils.truncate("hello", 3));
    }

    @Test
    void countOccurrences_basic() {
        assertEquals(3, StringUtils.countOccurrences("abcabcabc", "abc"));
    }

    @Test
    void countOccurrences_noMatch() {
        assertEquals(0, StringUtils.countOccurrences("hello", "xyz"));
    }

    @Test
    void countOccurrences_nullInput() {
        assertEquals(0, StringUtils.countOccurrences(null, "a"));
    }

    @Test
    void estimateTokenCount_basic() {
        // 12 chars / 4 = 3 tokens
        assertEquals(3, StringUtils.estimateTokenCount("hello world!"));
    }

    @Test
    void estimateTokenCount_empty() {
        assertEquals(0, StringUtils.estimateTokenCount(""));
    }

    @Test
    void defaultIfBlank_returnsDefault() {
        assertEquals("default", StringUtils.defaultIfBlank(null, "default"));
        assertEquals("default", StringUtils.defaultIfBlank("", "default"));
    }

    @Test
    void defaultIfBlank_returnsValue() {
        assertEquals("value", StringUtils.defaultIfBlank("value", "default"));
    }
}
