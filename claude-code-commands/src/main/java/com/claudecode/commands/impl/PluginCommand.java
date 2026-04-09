package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;
import com.claudecode.services.plugins.BuiltinPlugin;
import com.claudecode.services.plugins.BuiltinPluginRegistry;

import java.util.List;

/**
 * /plugin — manage built-in plugins.
 * Supports enable, disable, and list operations for plugins.
 */
public class PluginCommand implements Command {

    private final BuiltinPluginRegistry registry;

    /**
     * Creates PluginCommand with a registry.
     */
    public PluginCommand(BuiltinPluginRegistry registry) {
        this.registry = registry;
    }

    /**
     * Creates PluginCommand without a registry (read-only mode with no plugins).
     */
    public PluginCommand() {
        this(new BuiltinPluginRegistry());
    }

    @Override
    public String name() {
        return "plugin";
    }

    @Override
    public String description() {
        return "Manage plugins";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        if (registry == null) {
            return CommandResult.of("Plugin system not initialized.");
        }

        String result = registry.handlePluginCommand(args);
        return CommandResult.of(result);
    }

    @Override
    public boolean isBridgeSafe() {
        return true;
    }
}
