package com.claudecode.commands.impl;

import com.claudecode.commands.*;

import java.util.List;

/**
 * /help — lists all available commands.
 */
public class HelpCommand implements Command {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() { return "help"; }

    @Override
    public String description() { return "Show available commands"; }

    @Override
    public List<String> aliases() { return List.of("?"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        List<Command> available = registry.getAvailable(context);
        StringBuilder sb = new StringBuilder("Available commands:\n");
        for (Command cmd : available) {
            sb.append("  /").append(cmd.name());
            if (!cmd.aliases().isEmpty()) {
                sb.append(" (").append(String.join(", ", cmd.aliases())).append(")");
            }
            sb.append(" — ").append(cmd.description()).append("\n");
        }
        return CommandResult.of(sb.toString().stripTrailing());
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
