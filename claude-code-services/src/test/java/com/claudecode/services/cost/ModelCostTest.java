package com.claudecode.services.cost;

import com.claudecode.core.message.Usage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelCostTest {

    @Test
    void sonnetPricing() {
        var pricing = ModelCost.getPricing("claude-sonnet-4-20250514");
        assertEquals(3.0, pricing.inputPerM());
        assertEquals(15.0, pricing.outputPerM());
    }

    @Test
    void opusPricing() {
        var pricing = ModelCost.getPricing("claude-opus-4");
        assertEquals(15.0, pricing.inputPerM());
        assertEquals(75.0, pricing.outputPerM());
    }

    @Test
    void haikuPricing() {
        var pricing = ModelCost.getPricing("claude-haiku-3");
        assertEquals(0.25, pricing.inputPerM());
        assertEquals(1.25, pricing.outputPerM());
    }

    @Test
    void unknownModelDefaultsToSonnet() {
        var pricing = ModelCost.getPricing("unknown-model");
        assertEquals(3.0, pricing.inputPerM());
    }

    @Test
    void nullModelDefaultsToSonnet() {
        var pricing = ModelCost.getPricing(null);
        assertEquals(3.0, pricing.inputPerM());
    }

    @Test
    void getCostCalculatesCorrectly() {
        Usage usage = new Usage(1_000_000, 1_000_000, 0, 0);
        // Sonnet: $3 input + $15 output = $18
        double cost = ModelCost.getCost("claude-sonnet-4", usage);
        assertEquals(18.0, cost, 0.001);
    }

    @Test
    void getCostForOpus() {
        Usage usage = new Usage(1_000_000, 1_000_000, 0, 0);
        // Opus: $15 input + $75 output = $90
        double cost = ModelCost.getCost("claude-opus-4", usage);
        assertEquals(90.0, cost, 0.001);
    }

    @Test
    void getCostForHaiku() {
        Usage usage = new Usage(1_000_000, 1_000_000, 0, 0);
        // Haiku: $0.25 input + $1.25 output = $1.50
        double cost = ModelCost.getCost("claude-haiku-3", usage);
        assertEquals(1.50, cost, 0.001);
    }

    @Test
    void getCostWithZeroUsage() {
        assertEquals(0.0, ModelCost.getCost("sonnet", Usage.EMPTY));
    }

    @Test
    void getCostWithNullUsage() {
        assertEquals(0.0, ModelCost.getCost("sonnet", null));
    }
}
