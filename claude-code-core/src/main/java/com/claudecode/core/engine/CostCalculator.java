package com.claudecode.core.engine;

import com.claudecode.core.message.Usage;

/**
 * Calculates USD cost based on token usage and model pricing.
 * Pricing is per million tokens.
 */
public class CostCalculator {

    // Default pricing for Claude Sonnet (per million tokens)
    private static final double DEFAULT_INPUT_COST_PER_M = 3.0;
    private static final double DEFAULT_OUTPUT_COST_PER_M = 15.0;
    private static final double DEFAULT_CACHE_WRITE_COST_PER_M = 3.75;
    private static final double DEFAULT_CACHE_READ_COST_PER_M = 0.30;

    private final double inputCostPerM;
    private final double outputCostPerM;
    private final double cacheWriteCostPerM;
    private final double cacheReadCostPerM;

    public CostCalculator() {
        this(DEFAULT_INPUT_COST_PER_M, DEFAULT_OUTPUT_COST_PER_M,
             DEFAULT_CACHE_WRITE_COST_PER_M, DEFAULT_CACHE_READ_COST_PER_M);
    }

    public CostCalculator(double inputCostPerM, double outputCostPerM,
                           double cacheWriteCostPerM, double cacheReadCostPerM) {
        this.inputCostPerM = inputCostPerM;
        this.outputCostPerM = outputCostPerM;
        this.cacheWriteCostPerM = cacheWriteCostPerM;
        this.cacheReadCostPerM = cacheReadCostPerM;
    }

    /**
     * Calculates the USD cost for the given token usage.
     */
    public double calculateCost(Usage usage) {
        if (usage == null) {
            return 0.0;
        }
        double inputCost = (usage.inputTokens() / 1_000_000.0) * inputCostPerM;
        double outputCost = (usage.outputTokens() / 1_000_000.0) * outputCostPerM;
        double cacheWriteCost = (usage.cacheCreationInputTokens() / 1_000_000.0) * cacheWriteCostPerM;
        double cacheReadCost = (usage.cacheReadInputTokens() / 1_000_000.0) * cacheReadCostPerM;
        return inputCost + outputCost + cacheWriteCost + cacheReadCost;
    }

    /**
     * Returns pricing for a given model name.
     */
    public static CostCalculator forModel(String model) {
        if (model == null) {
            return new CostCalculator();
        }
        // Opus models
        if (model.contains("opus")) {
            return new CostCalculator(15.0, 75.0, 18.75, 1.50);
        }
        // Haiku models
        if (model.contains("haiku")) {
            return new CostCalculator(0.25, 1.25, 0.30, 0.03);
        }
        // Default: Sonnet pricing
        return new CostCalculator();
    }
}
