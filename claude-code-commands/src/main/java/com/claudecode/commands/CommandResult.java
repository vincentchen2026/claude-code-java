package com.claudecode.commands;

/**
 * Result of executing a slash command.
 *
 * @param output     text output to display to the user
 * @param shouldExit whether the REPL should exit after this command
 */
public record CommandResult(String output, boolean shouldExit) {

    /** Create a result with output that does not exit. */
    public static CommandResult of(String output) {
        return new CommandResult(output, false);
    }

    /** Create a result that signals the REPL to exit. */
    public static CommandResult exit(String output) {
        return new CommandResult(output, true);
    }
}
