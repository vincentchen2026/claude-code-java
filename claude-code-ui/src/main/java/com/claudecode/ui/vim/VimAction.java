package com.claudecode.ui.vim;

/**
 * Result of processing a key in the Vim state machine.
 * Task 67 enhancements: Added new action types for count, selection, command, quit, save.
 */
public record VimAction(Type type, VimMode newMode, VimOperator operator, int cursorPos,
                        int pendingCount, int selectionStart, int selectionEnd,
                        String command) {

    public enum Type {
        NONE,
        MODE_CHANGE,
        BUFFER_CHANGED,
        CURSOR_MOVED,
        OPERATOR_PENDING,
        WAITING_FOR_CHAR,
        COUNT_PENDING,
        SELECTION_CHANGED,
        COMMAND_BUFFER_CHANGED,
        QUIT,
        QUIT_FORCE,
        SAVE,
        SAVE_AND_QUIT,
        CLEAR_SEARCH
    }

    public static VimAction none() {
        return new VimAction(Type.NONE, null, null, -1, 0, -1, -1, null);
    }

    public static VimAction modeChange(VimMode mode) {
        return new VimAction(Type.MODE_CHANGE, mode, null, -1, 0, -1, -1, null);
    }

    public static VimAction bufferChanged() {
        return new VimAction(Type.BUFFER_CHANGED, null, null, -1, 0, -1, -1, null);
    }

    public static VimAction cursorMoved(int pos) {
        return new VimAction(Type.CURSOR_MOVED, null, null, pos, 0, -1, -1, null);
    }

    public static VimAction operatorPending(VimOperator op) {
        return new VimAction(Type.OPERATOR_PENDING, null, op, -1, 0, -1, -1, null);
    }

    public static VimAction waitingForChar() {
        return new VimAction(Type.WAITING_FOR_CHAR, null, null, -1, 0, -1, -1, null);
    }

    // Task 67.1: Count pending
    public static VimAction countPending(int count) {
        return new VimAction(Type.COUNT_PENDING, null, null, -1, count, -1, -1, null);
    }

    // Task 67.3: Selection changed
    public static VimAction selectionChanged(int start, int end) {
        return new VimAction(Type.SELECTION_CHANGED, null, null, -1, 0, start, end, null);
    }

    // Task 67.4: Command buffer changed
    public static VimAction commandBufferChanged() {
        return new VimAction(Type.COMMAND_BUFFER_CHANGED, null, null, -1, 0, -1, -1, null);
    }

    // Task 67.4: Command actions
    public static VimAction quit() {
        return new VimAction(Type.QUIT, null, null, -1, 0, -1, -1, null);
    }

    public static VimAction quitForce() {
        return new VimAction(Type.QUIT_FORCE, null, null, -1, 0, -1, -1, null);
    }

    public static VimAction save() {
        return new VimAction(Type.SAVE, null, null, -1, 0, -1, -1, null);
    }

    public static VimAction saveAndQuit() {
        return new VimAction(Type.SAVE_AND_QUIT, null, null, -1, 0, -1, -1, null);
    }

    public static VimAction clearSearch() {
        return new VimAction(Type.CLEAR_SEARCH, null, null, -1, 0, -1, -1, null);
    }
}
