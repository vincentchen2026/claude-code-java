package com.claudecode.commands;

import java.util.List;

/**
 * Slash command interface.
 * Each command handles a specific /command invocation in the REPL.
 */
public interface Command {

    /** Command name without the leading slash, e.g. "help", "exit". */
    String name();

    /** Human-readable description shown in /help output. */
    String description();

    /** Alternative names for this command (e.g. "quit" for "exit"). */
    default List<String> aliases() {
        return List.of();
    }

    /** Execute the command with the given context and argument string. */
    CommandResult execute(CommandContext context, String args);

    /** Whether this command is available in the current environment. */
    default boolean isAvailable(CommandContext context) {
        return true;
    }

    /** Whether this command is safe to use in remote/bridge mode. */
    default boolean isBridgeSafe() {
        return false;
    }
}
