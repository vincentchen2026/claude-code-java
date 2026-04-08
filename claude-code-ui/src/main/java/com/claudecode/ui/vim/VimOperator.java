package com.claudecode.ui.vim;

/**
 * Vim operators that act on motions/text objects.
 */
public enum VimOperator {
    DELETE('d'),
    CHANGE('c'),
    YANK('y');

    private final char key;

    VimOperator(char key) {
        this.key = key;
    }

    public char key() {
        return key;
    }

    public static VimOperator fromChar(char c) {
        return switch (c) {
            case 'd' -> DELETE;
            case 'c' -> CHANGE;
            case 'y' -> YANK;
            default -> null;
        };
    }
}
