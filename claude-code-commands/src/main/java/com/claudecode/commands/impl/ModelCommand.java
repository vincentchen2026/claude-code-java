package com.claudecode.commands.impl;

import com.claudecode.commands.*;

/**
 * /model — shows or changes the current model.
 */
public class ModelCommand implements Command {

    @Override
    public String name() { return "model"; }

    @Override
    public String description() { return "Show or change the current model"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        if (args == null || args.isBlank()) {
            return CommandResult.of("Current model: " + context.model());
        }
        String newModel = args.trim();
        context.setModel().accept(newModel);
        return CommandResult.of("Model changed to: " + newModel);
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
