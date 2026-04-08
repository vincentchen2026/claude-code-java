package com.claudecode.commands.impl;

import com.claudecode.commands.*;

/**
 * /clear — clears the conversation history.
 */
public class ClearCommand implements Command {

    @Override
    public String name() { return "clear"; }

    @Override
    public String description() { return "Clear conversation history"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        context.clearMessages().run();
        return CommandResult.of("Conversation cleared.");
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
