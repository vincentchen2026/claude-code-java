package com.claudecode.commands;

import java.util.*;

/**
 * Registry for slash commands. Supports registration, lookup by name/alias,
 * and dispatching input strings to the appropriate command.
 */
public class CommandRegistry {

    private final Map<String, Command> commandsByName = new LinkedHashMap<>();
    private final Map<String, Command> aliasMap = new HashMap<>();

    /**
     * Register a command. Overwrites any existing command with the same name.
     */
    public void register(Command cmd) {
        Objects.requireNonNull(cmd, "command must not be null");
        commandsByName.put(cmd.name().toLowerCase(), cmd);
        for (String alias : cmd.aliases()) {
            aliasMap.put(alias.toLowerCase(), cmd);
        }
    }

    /**
     * Find a command by name or alias.
     */
    public Optional<Command> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String key = name.toLowerCase();
        Command cmd = commandsByName.get(key);
        if (cmd != null) return Optional.of(cmd);
        return Optional.ofNullable(aliasMap.get(key));
    }

    /**
     * Get all registered commands (primary names only, no alias duplicates).
     */
    public List<Command> getAll() {
        return List.copyOf(commandsByName.values());
    }

    /**
     * Get all commands available in the given context.
     */
    public List<Command> getAvailable(CommandContext context) {
        return commandsByName.values().stream()
            .filter(c -> c.isAvailable(context))
            .toList();
    }

    /**
     * Parse and dispatch a raw slash command input string.
     * Input format: "/command arg1 arg2 ..."
     *
     * @return the command result, or a result indicating unknown command
     */
    public CommandResult dispatch(String input, CommandContext context) {
        if (input == null || input.isBlank()) {
            return CommandResult.of("Empty command.");
        }

        String trimmed = input.trim();
        if (!trimmed.startsWith("/")) {
            return CommandResult.of("Not a command: " + trimmed);
        }

        ParsedCommand parsed = parseInput(trimmed);
        Optional<Command> cmd = find(parsed.name());

        if (cmd.isEmpty()) {
            return CommandResult.of("Unknown command: /" + parsed.name()
                + ". Type /help for available commands.");
        }

        Command command = cmd.get();
        if (!command.isAvailable(context)) {
            return CommandResult.of("Command /" + command.name()
                + " is not available in the current context.");
        }

        return command.execute(context, parsed.args());
    }

    /**
     * Parse a slash command input into command name and args.
     */
    static ParsedCommand parseInput(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedCommand("", "");
        }
        String trimmed = input.trim();
        // Strip leading slash
        String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;

        int spaceIdx = withoutSlash.indexOf(' ');
        if (spaceIdx < 0) {
            return new ParsedCommand(withoutSlash.toLowerCase(), "");
        }
        String name = withoutSlash.substring(0, spaceIdx).toLowerCase();
        String args = withoutSlash.substring(spaceIdx + 1).trim();
        return new ParsedCommand(name, args);
    }

    /**
     * Parsed slash command: name (without slash) and argument string.
     */
    record ParsedCommand(String name, String args) {}
}
