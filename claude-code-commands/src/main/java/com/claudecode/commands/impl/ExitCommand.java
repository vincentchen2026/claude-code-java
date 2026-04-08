package com.claudecode.commands.impl;

import com.claudecode.commands.*;

import java.util.List;

/**
 * /exit — exits the REPL.
 */
public class ExitCommand implements Command {

    @Override
    public String name() { return "exit"; }

    @Override
    public String description() { return "Exit the application"; }

    @Override
    public List<String> aliases() { return List.of("quit"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        return CommandResult.exit("Goodbye!");
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
