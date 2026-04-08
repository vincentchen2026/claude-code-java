package com.claudecode.ui;

/**
 * Common ANSI text styles.
 */
public enum AnsiStyle {
    BOLD("\u001B[1m", "\u001B[22m"),
    DIM("\u001B[2m", "\u001B[22m"),
    ITALIC("\u001B[3m", "\u001B[23m"),
    UNDERLINE("\u001B[4m", "\u001B[24m"),
    STRIKETHROUGH("\u001B[9m", "\u001B[29m");

    private final String on;
    private final String off;

    AnsiStyle(String on, String off) {
        this.on = on;
        this.off = off;
    }

    public String on() {
        return on;
    }

    public String off() {
        return off;
    }
}
