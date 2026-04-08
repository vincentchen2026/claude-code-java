package com.claudecode.core.engine;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AbortControllerTest {

    @Test
    void initiallyNotAborted() {
        var controller = new AbortController();
        assertFalse(controller.isAborted());
    }

    @Test
    void abortSetsFlag() {
        var controller = new AbortController();
        controller.abort();
        assertTrue(controller.isAborted());
    }

    @Test
    void abortIsIdempotent() {
        var controller = new AbortController();
        controller.abort();
        controller.abort();
        controller.abort();
        assertTrue(controller.isAborted());
    }

    @Test
    void callbackInvokedOnAbort() {
        var controller = new AbortController();
        var called = new AtomicBoolean(false);
        controller.onAbort(() -> called.set(true));

        assertFalse(called.get());
        controller.abort();
        assertTrue(called.get());
    }

    @Test
    void multipleCallbacksInvoked() {
        var controller = new AbortController();
        var counter = new AtomicInteger(0);
        controller.onAbort(counter::incrementAndGet);
        controller.onAbort(counter::incrementAndGet);
        controller.onAbort(counter::incrementAndGet);

        controller.abort();
        assertEquals(3, counter.get());
    }

    @Test
    void callbackInvokedOnlyOnce() {
        var controller = new AbortController();
        var counter = new AtomicInteger(0);
        controller.onAbort(counter::incrementAndGet);

        controller.abort();
        controller.abort();
        assertEquals(1, counter.get());
    }

    @Test
    void callbackRegisteredAfterAbortIsInvokedImmediately() {
        var controller = new AbortController();
        controller.abort();

        var called = new AtomicBoolean(false);
        controller.onAbort(() -> called.set(true));
        assertTrue(called.get());
    }

    @Test
    void throwIfAbortedDoesNothingWhenNotAborted() {
        var controller = new AbortController();
        assertDoesNotThrow(controller::throwIfAborted);
    }

    @Test
    void throwIfAbortedThrowsWhenAborted() {
        var controller = new AbortController();
        controller.abort();
        assertThrows(AbortException.class, controller::throwIfAborted);
    }

    @Test
    void callbackExceptionDoesNotPreventOtherCallbacks() {
        var controller = new AbortController();
        var secondCalled = new AtomicBoolean(false);

        controller.onAbort(() -> { throw new RuntimeException("boom"); });
        controller.onAbort(() -> secondCalled.set(true));

        controller.abort();
        assertTrue(secondCalled.get());
    }

    @Test
    void abortIsThreadSafe() throws InterruptedException {
        var controller = new AbortController();
        var counter = new AtomicInteger(0);
        var latch = new CountDownLatch(10);

        // 10 threads all try to abort concurrently
        for (int i = 0; i < 10; i++) {
            controller.onAbort(counter::incrementAndGet);
        }

        for (int i = 0; i < 10; i++) {
            Thread.ofVirtual().start(() -> {
                controller.abort();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(controller.isAborted());
        // Each callback should be invoked exactly once
        assertEquals(10, counter.get());
    }
}
