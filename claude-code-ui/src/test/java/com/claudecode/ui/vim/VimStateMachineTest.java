package com.claudecode.ui.vim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VimStateMachineTest {

    private VimStateMachine vim;

    @BeforeEach
    void setUp() {
        vim = new VimStateMachine();
    }

    @Test
    void startsInInsertMode() {
        assertEquals(VimMode.INSERT, vim.getMode());
    }

    @Test
    void escSwitchesToNormalMode() {
        VimAction action = vim.processKey((char) 27); // ESC
        assertEquals(VimMode.NORMAL, vim.getMode());
        assertEquals(VimAction.Type.MODE_CHANGE, action.type());
    }

    @Test
    void iSwitchesToInsertMode() {
        vim.processKey((char) 27); // ESC -> NORMAL
        VimAction action = vim.processKey('i');
        assertEquals(VimMode.INSERT, vim.getMode());
        assertEquals(VimAction.Type.MODE_CHANGE, action.type());
    }

    @Test
    void insertModeTypesCharacters() {
        vim.processKey('h');
        vim.processKey('e');
        vim.processKey('l');
        vim.processKey('l');
        vim.processKey('o');
        assertEquals("hello", vim.getBuffer());
        assertEquals(5, vim.getCursor());
    }

    @Test
    void motionH_movesLeft() {
        vim.setBuffer("hello");
        vim.processKey((char) 27); // ESC -> NORMAL
        // cursor should be at end-1 after ESC
        int startCursor = vim.getCursor();
        vim.processKey('h');
        assertTrue(vim.getCursor() < startCursor || startCursor == 0);
    }

    @Test
    void motionL_movesRight() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('0'); // go to start
        assertEquals(0, vim.getCursor());
        vim.processKey('l');
        assertEquals(1, vim.getCursor());
    }

    @Test
    void motionW_movesToNextWord() {
        vim.setBuffer("hello world");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('w');
        assertEquals(6, vim.getCursor()); // 'w' in "world"
    }

    @Test
    void motionB_movesToPrevWord() {
        vim.setBuffer("hello world");
        vim.processKey((char) 27);
        vim.processKey('$'); // end
        vim.processKey('b');
        assertEquals(6, vim.getCursor()); // 'w' in "world"
    }

    @Test
    void motion0_movesToLineStart() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('0');
        assertEquals(0, vim.getCursor());
    }

    @Test
    void motionDollar_movesToLineEnd() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('$');
        assertEquals(4, vim.getCursor());
    }

    @Test
    void operatorDD_deletesWholeLine() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('d');
        vim.processKey('d');
        assertEquals("", vim.getBuffer());
        assertEquals("hello", vim.getYankRegister());
    }

    @Test
    void operatorYY_yanksWholeLine() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('y');
        vim.processKey('y');
        assertEquals("hello", vim.getBuffer()); // unchanged
        assertEquals("hello", vim.getYankRegister());
    }

    @Test
    void operatorDW_deletesToNextWord() {
        vim.setBuffer("hello world");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('d');
        vim.processKey('w');
        // Should delete "hello " or "hello" + motion
        assertTrue(vim.getBuffer().length() < 11);
    }

    @Test
    void xDeletesCharUnderCursor() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('x');
        assertEquals("ello", vim.getBuffer());
    }

    @Test
    void pPastesAfterCursor() {
        vim.setBuffer("hllo");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('x'); // delete 'h', yank it
        vim.processKey('p'); // paste after cursor
        assertTrue(vim.getBuffer().contains("h"));
    }

    @Test
    void aSwitchesToInsertAfterCursor() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('0');
        int cursorBefore = vim.getCursor();
        vim.processKey('a');
        assertEquals(VimMode.INSERT, vim.getMode());
        assertEquals(cursorBefore + 1, vim.getCursor());
    }

    @Test
    void capitalA_movesToEndAndInserts() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('A');
        assertEquals(VimMode.INSERT, vim.getMode());
        assertEquals(5, vim.getCursor());
    }

    @Test
    void findF_movesToCharForward() {
        vim.setBuffer("hello world");
        vim.processKey((char) 27);
        vim.processKey('0');
        VimAction action = vim.processKey('f');
        assertEquals(VimAction.Type.WAITING_FOR_CHAR, action.type());
        vim.processFindChar('w');
        assertEquals(6, vim.getCursor());
    }

    @Test
    void findT_movesBeforeCharForward() {
        vim.setBuffer("hello world");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('t');
        vim.processFindChar('w');
        assertEquals(5, vim.getCursor());
    }

    @Test
    void reset_clearsState() {
        vim.setBuffer("hello");
        vim.processKey((char) 27);
        vim.reset();
        assertEquals(VimMode.INSERT, vim.getMode());
        assertEquals("", vim.getBuffer());
        assertEquals(0, vim.getCursor());
    }

    @Test
    void motionCaret_movesToFirstNonBlank() {
        vim.setBuffer("  hello");
        vim.processKey((char) 27);
        vim.processKey('^');
        assertEquals(2, vim.getCursor());
    }

    @Test
    void changeCW_deletesWordAndEntersInsert() {
        vim.setBuffer("hello world");
        vim.processKey((char) 27);
        vim.processKey('0');
        vim.processKey('c');
        vim.processKey('w');
        assertEquals(VimMode.INSERT, vim.getMode());
        assertTrue(vim.getBuffer().length() < 11);
    }

    @Test
    void wordMotionHelpers() {
        assertEquals(6, VimStateMachine.nextWordStart("hello world", 0));
        assertEquals(0, VimStateMachine.prevWordStart("hello world", 6));
        assertTrue(VimStateMachine.isWordChar('a'));
        assertTrue(VimStateMachine.isWordChar('_'));
        assertFalse(VimStateMachine.isWordChar(' '));
    }
}
