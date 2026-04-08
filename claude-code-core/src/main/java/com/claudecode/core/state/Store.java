package com.claudecode.core.state;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * Thread-safe publish-subscribe state store.
 * <p>
 * Corresponds to the TS {@code createStore} in {@code src/state/store.ts}.
 * <p>
 * CP-5 invariants:
 * <ul>
 *   <li>If updater returns same reference as current state, no listener is triggered</li>
 *   <li>State updates are atomic (listeners see either old or new state, never partial)</li>
 *   <li>Concurrent setState calls are serialized via ReentrantLock</li>
 * </ul>
 *
 * @param <T> the state type
 */
public class Store<T> {

    private volatile T state;
    private final Set<Runnable> listeners = ConcurrentHashMap.newKeySet();
    private final BiConsumer<T, T> onChange;
    private final ReentrantLock updateLock = new ReentrantLock();

    /**
     * Creates a new Store with the given initial state and optional onChange callback.
     *
     * @param initialState the initial state value
     * @param onChange     callback invoked with (newState, oldState) on state change; may be null
     */
    public Store(T initialState, BiConsumer<T, T> onChange) {
        this.state = initialState;
        this.onChange = onChange;
    }

    /**
     * Returns the current state. This read is lock-free (volatile read).
     *
     * @return the current state
     */
    public T getState() {
        return state;
    }

    /**
     * Atomically updates the state. If the updater returns the same reference
     * as the current state, no listeners are triggered (CP-5).
     * <p>
     * Concurrent calls are serialized via ReentrantLock.
     *
     * @param updater a function that receives the current state and returns the new state
     */
    public void setState(UnaryOperator<T> updater) {
        updateLock.lock();
        try {
            T prev = state;
            T next = updater.apply(prev);
            if (next == prev) {
                return; // Reference equality — skip listeners
            }
            state = next;
            if (onChange != null) {
                onChange.accept(next, prev);
            }
            for (Runnable listener : listeners) {
                listener.run();
            }
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Subscribes a listener that is called whenever the state changes.
     * Returns an unsubscribe function.
     *
     * @param listener the listener to invoke on state changes
     * @return a Runnable that, when called, removes the listener
     */
    public Runnable subscribe(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
