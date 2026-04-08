package com.claudecode.services.cost;

import com.claudecode.core.engine.CostCalculator;
import com.claudecode.core.message.Usage;

import java.util.Map;

/**
 * Model pricing table. Maps model names to per-million-token pricing
 * and provides cost calculation for any supported model.
 */
public final class ModelCost {

    /**
     * Pricing entry: input/output/cacheWrite/cacheRead costs per million tokens.
     */
    public record Pricing(double inputPerM, double outputPerM,
                          double cacheWritePerM, double cacheReadPerM) {
    }

    private static final Map<String, Pricing> PRICING_TABLE = Map.of(
        "sonnet", new Pricing(3.0, 15.0, 3.75, 0.30),
        "opus",   new Pricing(15.0, 75.0, 18.75, 1.50),
        "haiku",  new Pricing(0.25, 1.25, 0.30, 0.03)
    );

    private ModelCost() {
    }

    /**
     * Returns the pricing for a model name. Falls back to Sonnet pricing
     * if the model is unknown.
     */
    public static Pricing getPricing(String model) {
        if (model != null) {
            String lower = model.toLowerCase();
            for (var entry : PRICING_TABLE.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return PRICING_TABLE.get("sonnet");
    }

    /**
     * Calculates the USD cost for the given model and usage.
     * Delegates to {@link CostCalculator#forModel(String)}.
     */
    public static double getCost(String model, Usage usage) {
        return CostCalculator.forModel(model).calculateCost(usage);
    }
}
