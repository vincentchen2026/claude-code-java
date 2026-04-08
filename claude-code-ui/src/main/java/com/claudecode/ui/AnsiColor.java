package com.claudecode.ui;

/**
 * Common ANSI foreground colors.
 */
public enum AnsiColor {
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    MAGENTA("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    GRAY("\u001B[90m"),
    DIM("\u001B[2m");

    private final String code;

    AnsiColor(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
