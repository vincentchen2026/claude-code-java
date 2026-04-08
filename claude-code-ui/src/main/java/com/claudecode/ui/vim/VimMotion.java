package com.claudecode.ui.vim;

/**
 * Vim motions for cursor movement.
 */
public enum VimMotion {
    LEFT('h'),
    DOWN('j'),
    UP('k'),
    RIGHT('l'),
    WORD_FORWARD('w'),
    WORD_BACK('b'),
    WORD_END('e'),
    LINE_START('0'),
    FIRST_NON_BLANK('^'),
    LINE_END('$');

    private final char key;

    VimMotion(char key) {
        this.key = key;
    }

    public char key() {
        return key;
    }

    public static VimMotion fromChar(char c) {
        return switch (c) {
            case 'h' -> LEFT;
            case 'j' -> DOWN;
            case 'k' -> UP;
            case 'l' -> RIGHT;
            case 'w' -> WORD_FORWARD;
            case 'b' -> WORD_BACK;
            case 'e' -> WORD_END;
            case '0' -> LINE_START;
            case '^' -> FIRST_NON_BLANK;
            case '$' -> LINE_END;
            default -> null;
        };
    }
}
