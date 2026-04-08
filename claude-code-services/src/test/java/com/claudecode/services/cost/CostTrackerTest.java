package com.claudecode.services.cost;

import com.claudecode.core.message.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class CostTrackerTest {

    private CostTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new CostTracker("claude-sonnet-4-20250514");
    }

    @Test
    void startsWithEmptyUsage() {
        assertEquals(Usage.EMPTY, tracker.getTotalUsage());
        assertEquals(0.0, tracker.getTotalCost());
    }

    @Test
    void addUsageAccumulates() {
        tracker.addUsage(new Usage(1000, 500, 0, 0));
        tracker.addUsage(new Usage(2000, 1000, 0, 0));

        Usage total = tracker.getTotalUsage();
        assertEquals(3000, total.inputTokens());
        assertEquals(1500, total.outputTokens());
    }

    @Test
    void addNullUsageIsNoOp() {
        tracker.addUsage(null);
        assertEquals(Usage.EMPTY, tracker.getTotalUsage());
    }

    @Test
    void totalCostUsesSonnetPricing() {
        // 1M input tokens at $3/M = $3.00
        tracker.addUsage(new Usage(1_000_000, 0, 0, 0));
        assertEquals(3.0, tracker.getTotalCost(), 0.001);
    }

    @Test
    void totalCostUsesOpusPricing() {
        tracker = new CostTracker("claude-opus-4");
        // 1M input tokens at $15/M = $15.00
        tracker.addUsage(new Usage(1_000_000, 0, 0, 0));
        assertEquals(15.0, tracker.getTotalCost(), 0.001);
    }

    @Test
    void totalCostUsesHaikuPricing() {
        tracker = new CostTracker("claude-haiku-3");
        // 1M input tokens at $0.25/M = $0.25
        tracker.addUsage(new Usage(1_000_000, 0, 0, 0));
        assertEquals(0.25, tracker.getTotalCost(), 0.001);
    }

    @Test
    void resetClearsCounters() {
        tracker.addUsage(new Usage(1000, 500, 0, 0));
        tracker.reset();

        assertEquals(Usage.EMPTY, tracker.getTotalUsage());
        assertEquals(0.0, tracker.getTotalCost());
    }

    @Test
    void setModelChangesPricing() {
        tracker.addUsage(new Usage(1_000_000, 0, 0, 0));
        assertEquals(3.0, tracker.getTotalCost(), 0.001); // Sonnet

        tracker.setModel("claude-opus-4");
        assertEquals(15.0, tracker.getTotalCost(), 0.001); // Opus
    }

    @Test
    void threadSafetyUnderConcurrentAdds() throws InterruptedException {
        int threads = 10;
        int addsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < addsPerThread; i++) {
                    tracker.addUsage(new Usage(1, 1, 0, 0));
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        Usage total = tracker.getTotalUsage();
        assertEquals(threads * addsPerThread, total.inputTokens());
        assertEquals(threads * addsPerThread, total.outputTokens());
    }

    @Test
    void defaultConstructorUsesSonnet() {
        CostTracker defaultTracker = new CostTracker();
        defaultTracker.addUsage(new Usage(1_000_000, 0, 0, 0));
        assertEquals(3.0, defaultTracker.getTotalCost(), 0.001);
    }

    @Test
    void cacheTokensIncludedInCost() {
        // Sonnet: cacheWrite=$3.75/M, cacheRead=$0.30/M
        tracker.addUsage(new Usage(0, 0, 1_000_000, 1_000_000));
        double expected = 3.75 + 0.30;
        assertEquals(expected, tracker.getTotalCost(), 0.001);
    }
}
