package com.claudecode.services.cost;

import com.claudecode.core.engine.CostCalculator;
import com.claudecode.core.message.Usage;

/**
 * Accumulates token usage across a session and computes running USD cost.
 * Thread-safe via synchronized methods.
 */
public class CostTracker {

    private final Object lock = new Object();
    private volatile Usage totalUsage = Usage.EMPTY;
    private volatile String model;

    public CostTracker() {
        this("claude-sonnet-4-20250514");
    }

    public CostTracker(String model) {
        this.model = model != null ? model : "claude-sonnet-4-20250514";
    }

    /**
     * Adds the given usage to the running total.
     */
    public void addUsage(Usage usage) {
        if (usage == null) {
            return;
        }
        synchronized (lock) {
            totalUsage = totalUsage.add(usage);
        }
    }

    /**
     * Returns the accumulated usage for this session.
     */
    public Usage getTotalUsage() {
        return totalUsage;
    }

    /**
     * Returns the estimated USD cost for accumulated usage.
     */
    public double getTotalCost() {
        return CostCalculator.forModel(model).calculateCost(totalUsage);
    }

    /**
     * Resets all counters to zero.
     */
    public void reset() {
        synchronized (lock) {
            totalUsage = Usage.EMPTY;
        }
    }

    /**
     * Updates the model used for cost calculation.
     */
    public void setModel(String model) {
        this.model = model != null ? model : "claude-sonnet-4-20250514";
    }

    /**
     * Returns the current model name.
     */
    public String getModel() {
        return model;
    }
}
