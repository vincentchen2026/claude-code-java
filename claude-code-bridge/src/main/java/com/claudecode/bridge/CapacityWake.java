package com.claudecode.bridge;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple capacity wake signal.
 * Used to notify the bridge that capacity is available for new work.
 */
public class CapacityWake {

    private final AtomicBoolean signaled = new AtomicBoolean(false);

    /** Signals that capacity is available. */
    public void signal() {
        signaled.set(true);
    }

    /** Checks and clears the signal. Returns true if it was signaled. */
    public boolean checkAndClear() {
        return signaled.getAndSet(false);
    }

    /** Returns whether the signal is currently set (without clearing). */
    public boolean isSignaled() {
        return signaled.get();
    }

    /** Resets the signal. */
    public void reset() {
        signaled.set(false);
    }
}
