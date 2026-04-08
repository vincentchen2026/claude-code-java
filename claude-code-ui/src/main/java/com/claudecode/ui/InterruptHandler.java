package com.claudecode.ui;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles Ctrl+C (SIGINT) interrupt behavior for the REPL.
 *
 * Behavior:
 * - During API call: calls QueryEngine.interrupt()
 * - During input: clears current line (returns null from readLine)
 * - Double Ctrl+C within threshold: exits REPL
 */
public class InterruptHandler {

    /** Time window for double Ctrl+C detection (milliseconds). */
    private static final long DOUBLE_CTRL_C_THRESHOLD_MS = 1000;

    private final AtomicLong lastInterruptTime = new AtomicLong(0);
    private final AtomicReference<Runnable> apiInterruptAction = new AtomicReference<>();
    private volatile boolean exitRequested = false;

    /**
     * State of the REPL for interrupt handling.
     */
    public enum ReplState {
        /** Waiting for user input */
        INPUT,
        /** Waiting for API response */
        API_CALL
    }

    private volatile ReplState currentState = ReplState.INPUT;

    /**
     * Register the action to call when interrupting an API call.
     * Typically this is QueryEngine::interrupt.
     */
    public void setApiInterruptAction(Runnable action) {
        apiInterruptAction.set(action);
    }

    /**
     * Set the current REPL state.
     */
    public void setState(ReplState state) {
        this.currentState = state;
    }

    /**
     * Get the current REPL state.
     */
    public ReplState getState() {
        return currentState;
    }

    /**
     * Handle a Ctrl+C signal. Returns the action that should be taken.
     */
    public InterruptAction handleInterrupt() {
        long now = System.currentTimeMillis();
        long lastTime = lastInterruptTime.getAndSet(now);

        // Double Ctrl+C detection
        if (now - lastTime < DOUBLE_CTRL_C_THRESHOLD_MS) {
            exitRequested = true;
            return InterruptAction.EXIT;
        }

        if (currentState == ReplState.API_CALL) {
            Runnable action = apiInterruptAction.get();
            if (action != null) {
                action.run();
            }
            return InterruptAction.CANCEL_API;
        }

        // During input: clear current line
        return InterruptAction.CLEAR_LINE;
    }

    /**
     * Returns true if exit has been requested (double Ctrl+C).
     */
    public boolean isExitRequested() {
        return exitRequested;
    }

    /**
     * Reset the exit request flag.
     */
    public void resetExitRequest() {
        exitRequested = false;
        lastInterruptTime.set(0);
    }

    /**
     * Actions that can result from an interrupt.
     */
    public enum InterruptAction {
        /** Cancel the current API call */
        CANCEL_API,
        /** Clear the current input line */
        CLEAR_LINE,
        /** Exit the REPL */
        EXIT
    }
}
