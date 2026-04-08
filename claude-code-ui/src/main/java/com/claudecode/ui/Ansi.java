package com.claudecode.ui;

/**
 * ANSI styling utility. Wraps text with ANSI escape sequences.
 * Auto-detects color support and falls back to plain text on dumb terminals.
 */
public final class Ansi {

    private static final String RESET = "\u001B[0m";
    private static final boolean COLOR_SUPPORTED = detectColorSupport();

    private Ansi() {}

    /**
     * Apply one or more styles to text.
     */
    public static String styled(String text, AnsiStyle... styles) {
        if (!COLOR_SUPPORTED || styles.length == 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (AnsiStyle style : styles) {
            sb.append(style.on());
        }
        sb.append(text);
        sb.append(RESET);
        return sb.toString();
    }

    /**
     * Apply a foreground color to text.
     */
    public static String colored(String text, AnsiColor color) {
        if (!COLOR_SUPPORTED) {
            return text;
        }
        return color.code() + text + RESET;
    }

    /**
     * Apply a foreground color and styles to text.
     */
    public static String styled(String text, AnsiColor color, AnsiStyle... styles) {
        if (!COLOR_SUPPORTED) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(color.code());
        for (AnsiStyle style : styles) {
            sb.append(style.on());
        }
        sb.append(text);
        sb.append(RESET);
        return sb.toString();
    }

    /**
     * Returns true if the current terminal supports ANSI colors.
     */
    public static boolean isColorSupported() {
        return COLOR_SUPPORTED;
    }

    static boolean detectColorSupport() {
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return false;
        }
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null && !noColor.isEmpty()) {
            return false;
        }
        // Most modern terminals support color
        return term != null || System.getenv("COLORTERM") != null
                || System.console() != null;
    }
}
