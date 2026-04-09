package com.claudecode.commands.impl;

import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;
import com.claudecode.services.plugins.BuiltinPlugin;
import com.claudecode.services.plugins.BuiltinPluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginCommandTest {

    private BuiltinPluginRegistry registry;
    private PluginCommand command;
    private CommandContext context;

    @BeforeEach
    void setUp() {
        registry = new BuiltinPluginRegistry();
        command = new PluginCommand(registry);
        context = CommandContext.minimal();
    }

    @Test
    void name() {
        assertEquals("plugin", command.name());
    }

    @Test
    void description() {
        assertEquals("Manage plugins", command.description());
    }

    @Test
    void aliases() {
        assertEquals(List.of(), command.aliases());
    }

    @Test
    void isBridgeSafe() {
        assertTrue(command.isBridgeSafe());
    }

    @Test
    void executeListShowsPlugins() {
        registry.register(new BuiltinPlugin("test", "A test plugin", true));

        CommandResult result = command.execute(context, "list");

        assertTrue(result.output().contains("test@builtin"));
        assertTrue(result.output().contains("enabled"));
    }

    @Test
    void executeEnablePlugin() {
        registry.register(new BuiltinPlugin("test", "Test", false));

        CommandResult result = command.execute(context, "enable test@builtin");

        assertTrue(result.output().contains("Enabled"));
        assertTrue(registry.isEnabled("test@builtin"));
    }

    @Test
    void executeDisablePlugin() {
        registry.register(new BuiltinPlugin("test", "Test", true));

        CommandResult result = command.execute(context, "disable test@builtin");

        assertTrue(result.output().contains("Disabled"));
        assertFalse(registry.isEnabled("test@builtin"));
    }

    @Test
    void executeEmptyArgsShowsList() {
        registry.register(new BuiltinPlugin("test", "Test", true));

        CommandResult result = command.execute(context, "");

        assertTrue(result.output().contains("test@builtin"));
    }

    @Test
    void executeUnknownActionShowsError() {
        CommandResult result = command.execute(context, "restart test@builtin");

        assertTrue(result.output().contains("Unknown action"));
    }

    @Test
    void executeNoPluginsShowsNoPlugins() {
        CommandResult result = command.execute(context, "list");

        assertTrue(result.output().contains("No plugins"));
    }

    @Test
    void defaultConstructorCreatesEmptyRegistry() {
        PluginCommand cmdWithDefaults = new PluginCommand();

        CommandResult result = cmdWithDefaults.execute(context, "list");

        assertTrue(result.output().contains("No plugins"));
    }
}