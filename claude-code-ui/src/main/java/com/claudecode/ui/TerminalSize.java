package com.claudecode.ui;

/**
 * Terminal dimensions.
 */
public record TerminalSize(int columns, int rows) {

    /** Default size used when terminal size cannot be determined. */
    public static final TerminalSize DEFAULT = new TerminalSize(80, 24);
}
