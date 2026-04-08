package com.claudecode.commands.impl;

import com.claudecode.commands.*;

import java.util.List;

/**
 * Base class for stub commands that are not yet implemented.
 */
public class StubCommand implements Command {

    private final String cmdName;
    private final String cmdDescription;
    private final List<String> cmdAliases;

    public StubCommand(String name, String description) {
        this(name, description, List.of());
    }

    public StubCommand(String name, String description, List<String> aliases) {
        this.cmdName = name;
        this.cmdDescription = description;
        this.cmdAliases = aliases;
    }

    @Override public String name() { return cmdName; }

    @Override public String description() { return cmdDescription; }

    @Override public List<String> aliases() { return cmdAliases; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        return CommandResult.of("/" + cmdName + ": Not yet implemented");
    }
}
