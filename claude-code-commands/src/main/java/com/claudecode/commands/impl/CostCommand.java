package com.claudecode.commands.impl;

import com.claudecode.commands.*;
import com.claudecode.core.message.Usage;

/**
 * /cost — shows token usage and estimated cost.
 */
public class CostCommand implements Command {

    @Override
    public String name() { return "cost"; }

    @Override
    public String description() { return "Show token usage and estimated cost"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        Usage usage = context.usageSupplier().get();
        double cost = context.costCalculator().applyAsDouble(usage);
        return CommandResult.of(String.format(
            "Total usage: %d input, %d output tokens. Estimated cost: $%.4f",
            usage.inputTokens(), usage.outputTokens(), cost));
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
