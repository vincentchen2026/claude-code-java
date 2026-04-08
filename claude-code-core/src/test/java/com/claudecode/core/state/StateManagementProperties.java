package com.claudecode.core.state;

import com.claudecode.core.task.TaskStateBase;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-5 State management invariant property-based tests.
 * <p>
 * Validates: Requirements CP-5 (状态管理不变量)
 * <p>
 * Properties verified:
 * - setState with identity updater triggers no listeners
 * - setState is atomic (concurrent reads see consistent state)
 * - Concurrent setState calls are serialized
 * - subscribe returns working unsubscribe function
 * - AppState with* methods preserve other fields
 */
class StateManagementProperties {

    // ========== Property: Identity updater triggers no listeners (CP-5) ==========

    /**
     * CP-5: If updater returns same reference as current state, no listener is triggered.
     * <p>
     * Validates: Requirements CP-5
     */
    @Property(tries = 100)
    void identityUpdaterTriggersNoListeners(
        @ForAll @IntRange(min = 0, max = 100) int initialValue
    ) {
        Store<Integer> store = new Store<>(initialValue, null);
        AtomicInteger callCount = new AtomicInteger(0);

        store.subscribe(callCount::incrementAndGet);

        // Identity updater returns same reference
        store.setState(s -> s);

        assertEquals(0, callCount.get(),
            "Identity updater must not trigger any listener");
        assertEquals(initialValue, store.getState());
    }

    // ========== Property: setState is atomic (CP-5) ==========

    /**
     * CP-5: State updates are atomic — concurrent reads see either old or new state, never partial.
     * <p>
     * Validates: Requirements CP-5
     */
    @Property(tries = 50)
    void setStateIsAtomic(
        @ForAll @IntRange(min = 2, max = 8) int numWriters,
        @ForAll @IntRange(min = 2, max = 8) int numReaders
    ) throws Exception {
        // Use a list as state — if updates were non-atomic, readers could see
        // a partially-constructed list
        List<Integer> initial = List.of(0, 0, 0);
        Store<List<Integer>> store = new Store<>(initial, null);

        int iterations = 50;
        CyclicBarrier barrier = new CyclicBarrier(numWriters + numReaders);
        ExecutorService executor = Executors.newFixedThreadPool(numWriters + numReaders);
        List<Future<?>> futures = new ArrayList<>();
        AtomicReference<AssertionError> failure = new AtomicReference<>();

        // Writers: set state to a list where all elements are the same value
        for (int w = 0; w < numWriters; w++) {
            final int writerId = w + 1;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return;
                }
                for (int i = 0; i < iterations; i++) {
                    final int val = writerId * 1000 + i;
                    store.setState(prev -> List.of(val, val, val));
                }
            }));
        }

        // Readers: verify all elements in the read state are the same value
        for (int r = 0; r < numReaders; r++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return;
                }
                for (int i = 0; i < iterations * 2; i++) {
                    List<Integer> snapshot = store.getState();
                    int first = snapshot.get(0);
                    for (int elem : snapshot) {
                        if (elem != first && failure.get() == null) {
                            failure.set(new AssertionError(
                                "Non-atomic state observed: " + snapshot));
                        }
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    // ========== Property: Concurrent setState calls are serialized (CP-5) ==========

    /**
     * CP-5: Concurrent setState calls are serialized via ReentrantLock.
     * <p>
     * Validates: Requirements CP-5
     */
    @Property(tries = 50)
    void concurrentSetStateCallsAreSerialized(
        @ForAll @IntRange(min = 2, max = 10) int numThreads
    ) throws Exception {
        int incrementsPerThread = 100;
        Store<Integer> store = new Store<>(0, null);

        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return;
                }
                for (int i = 0; i < incrementsPerThread; i++) {
                    store.setState(prev -> prev + 1);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        int expected = numThreads * incrementsPerThread;
        assertEquals(expected, store.getState(),
            "All increments must be applied — serialization ensures no lost updates");
    }

    // ========== Property: Subscribe returns working unsubscribe function (CP-5) ==========

    /**
     * CP-5: subscribe returns a working unsubscribe function.
     * <p>
     * Validates: Requirements CP-5
     */
    @Property(tries = 100)
    void subscribeReturnsWorkingUnsubscribe(
        @ForAll @IntRange(min = 1, max = 10) int updatesBeforeUnsub,
        @ForAll @IntRange(min = 1, max = 10) int updatesAfterUnsub
    ) {
        Store<Integer> store = new Store<>(0, null);
        AtomicInteger callCount = new AtomicInteger(0);

        Runnable unsubscribe = store.subscribe(callCount::incrementAndGet);

        // Updates before unsubscribe should trigger listener
        for (int i = 0; i < updatesBeforeUnsub; i++) {
            final int val = i + 1;
            store.setState(prev -> val);
        }
        int countBefore = callCount.get();
        assertEquals(updatesBeforeUnsub, countBefore,
            "Listener should be called for each state change before unsubscribe");

        // Unsubscribe
        unsubscribe.run();

        // Updates after unsubscribe should NOT trigger listener
        for (int i = 0; i < updatesAfterUnsub; i++) {
            final int val = updatesBeforeUnsub + i + 1;
            store.setState(prev -> val);
        }
        assertEquals(countBefore, callCount.get(),
            "Listener must not be called after unsubscribe");
    }

    // ========== Property: AppState with* methods preserve other fields ==========

    /**
     * CP-5: AppState with* methods preserve all other fields.
     * <p>
     * Validates: Requirements CP-5
     */
    @Property(tries = 100)
    void appStateWithMethodsPreserveOtherFields(
        @ForAll("appStates") AppState original
    ) {
        // withTasks
        Map<String, TaskStateBase> newTasks = Map.of("t1",
            new TaskStateBase("t1", "local_bash", "running", "test"));
        AppState afterTasks = original.withTasks(newTasks);
        assertEquals(newTasks, afterTasks.tasks());
        assertEquals(original.toolPermissionContext(), afterTasks.toolPermissionContext());
        assertEquals(original.fileHistory(), afterTasks.fileHistory());
        assertEquals(original.attribution(), afterTasks.attribution());
        assertEquals(original.fastMode(), afterTasks.fastMode());
        assertEquals(original.extra(), afterTasks.extra());

        // withFileHistory
        FileHistoryState newFh = new FileHistoryState(
            Set.of("/tmp/a.txt"), Set.of("/tmp/b.txt"), Map.of());
        AppState afterFh = original.withFileHistory(newFh);
        assertEquals(newFh, afterFh.fileHistory());
        assertEquals(original.toolPermissionContext(), afterFh.toolPermissionContext());
        assertEquals(original.tasks(), afterFh.tasks());
        assertEquals(original.attribution(), afterFh.attribution());
        assertEquals(original.fastMode(), afterFh.fastMode());
        assertEquals(original.extra(), afterFh.extra());

        // withAttribution
        AttributionState newAttr = new AttributionState(Map.of("key", "val"));
        AppState afterAttr = original.withAttribution(newAttr);
        assertEquals(newAttr, afterAttr.attribution());
        assertEquals(original.toolPermissionContext(), afterAttr.toolPermissionContext());
        assertEquals(original.tasks(), afterAttr.tasks());
        assertEquals(original.fileHistory(), afterAttr.fileHistory());
        assertEquals(original.fastMode(), afterAttr.fastMode());
        assertEquals(original.extra(), afterAttr.extra());

        // withFastMode
        boolean newFast = !original.fastMode();
        AppState afterFast = original.withFastMode(newFast);
        assertEquals(newFast, afterFast.fastMode());
        assertEquals(original.toolPermissionContext(), afterFast.toolPermissionContext());
        assertEquals(original.tasks(), afterFast.tasks());
        assertEquals(original.fileHistory(), afterFast.fileHistory());
        assertEquals(original.attribution(), afterFast.attribution());
        assertEquals(original.extra(), afterFast.extra());

        // withExtra
        Map<String, Object> newExtra = Map.of("foo", "bar");
        AppState afterExtra = original.withExtra(newExtra);
        assertEquals(newExtra, afterExtra.extra());
        assertEquals(original.toolPermissionContext(), afterExtra.toolPermissionContext());
        assertEquals(original.tasks(), afterExtra.tasks());
        assertEquals(original.fileHistory(), afterExtra.fileHistory());
        assertEquals(original.attribution(), afterExtra.attribution());
        assertEquals(original.fastMode(), afterExtra.fastMode());
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<AppState> appStates() {
        Arbitrary<ToolPermissionContext> permCtx = Arbitraries.of(
            ToolPermissionContext.EMPTY,
            new ToolPermissionContext("plan", Map.of(), List.of()),
            new ToolPermissionContext("auto", Map.of("bash", List.of("allow")), List.of("/tmp"))
        );

        Arbitrary<Map<String, TaskStateBase>> tasks = Arbitraries.of(
            Map.<String, TaskStateBase>of(),
            Map.of("task1", new TaskStateBase("task1", "local_bash", "pending", "desc"))
        );

        Arbitrary<FileHistoryState> fh = Arbitraries.of(
            FileHistoryState.EMPTY,
            new FileHistoryState(Set.of("a.txt"), Set.of("b.txt"), Map.of())
        );

        Arbitrary<AttributionState> attr = Arbitraries.of(
            AttributionState.EMPTY,
            new AttributionState(Map.of("src", "test"))
        );

        Arbitrary<Boolean> fast = Arbitraries.of(true, false);

        Arbitrary<Map<String, Object>> extra = Arbitraries.of(
            Map.<String, Object>of(),
            Map.of("key", "value")
        );

        return Combinators.combine(permCtx, tasks, fh, attr, fast, extra)
            .as(AppState::new);
    }
}
