package com.claudecode.core.engine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe abort signal controller.
 * Allows cooperative cancellation of long-running operations (e.g., API calls, query loops).
 * <p>
 * Modeled after the Web API AbortController pattern.
 */
public class AbortController {

    private final AtomicBoolean aborted = new AtomicBoolean(false);
    private final List<Runnable> callbacks = new CopyOnWriteArrayList<>();

    /**
     * Returns {@code true} if {@link #abort()} has been called.
     */
    public boolean isAborted() {
        return aborted.get();
    }

    /**
     * Signals abort. All registered callbacks are invoked exactly once.
     * Subsequent calls are no-ops.
     */
    public void abort() {
        if (aborted.compareAndSet(false, true)) {
            for (Runnable callback : callbacks) {
                try {
                    callback.run();
                } catch (Exception ignored) {
                    // Abort callbacks must not throw
                }
            }
        }
    }

    /**
     * Registers a callback to be invoked when {@link #abort()} is called.
     * If already aborted, the callback is invoked immediately.
     *
     * @param callback the callback to register
     */
    public void onAbort(Runnable callback) {
        callbacks.add(callback);
        // If already aborted, fire immediately
        if (aborted.get()) {
            try {
                callback.run();
            } catch (Exception ignored) {
                // Abort callbacks must not throw
            }
        }
    }

    /**
     * Throws {@link AbortException} if this controller has been aborted.
     */
    public void throwIfAborted() {
        if (aborted.get()) {
            throw new AbortException("Operation was aborted");
        }
    }
}
