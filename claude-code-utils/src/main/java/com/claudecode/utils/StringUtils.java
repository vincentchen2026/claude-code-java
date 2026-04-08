package com.claudecode.utils;

import java.util.Objects;

/**
 * String utility methods for Claude Code.
 */
public final class StringUtils {

    private StringUtils() {
        // utility class
    }

    /**
     * Checks if a string is null or blank (empty or whitespace only).
     */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Checks if a string is not null and not blank.
     */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    /**
     * Truncates a string to the specified max length, appending an ellipsis if truncated.
     *
     * @param s         the input string
     * @param maxLength maximum length (must be >= 3 if truncation may occur)
     * @return the truncated string, or the original if within limit
     */
    public static String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be non-negative");
        if (s.length() <= maxLength) return s;
        if (maxLength <= 3) return s.substring(0, maxLength);
        return s.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns the string if non-null, otherwise returns the default value.
     */
    public static String defaultIfBlank(String s, String defaultValue) {
        return isBlank(s) ? defaultValue : s;
    }

    /**
     * Counts occurrences of a substring within a string.
     */
    public static int countOccurrences(String text, String sub) {
        if (text == null || sub == null || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Estimates token count based on character count (rough: ~4 chars per token).
     */
    public static long estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() / 4;
    }
}
