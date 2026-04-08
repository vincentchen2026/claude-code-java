package com.claudecode.commands;

import com.claudecode.commands.impl.*;

/**
 * Factory that creates a CommandRegistry pre-populated with all default commands.
 */
public final class CommandFactory {

    private CommandFactory() {}

    /**
     * Create a registry with all built-in commands registered.
     */
    public static CommandRegistry createDefault() {
        CommandRegistry registry = new CommandRegistry();

        // P0 commands
        registry.register(new HelpCommand(registry));
        registry.register(new ExitCommand());
        registry.register(new ClearCommand());
        registry.register(new CompactCommand());
        registry.register(new ConfigCommand());
        registry.register(new ModelCommand());
        registry.register(new CostCommand());

        // P1 commands
        registry.register(new CommitCommand());
        registry.register(new DiffCommand());
        registry.register(new ReviewCommand());
        registry.register(new ResumeCommand());
        registry.register(new ShareCommand());
        registry.register(new ExportCommand());
        registry.register(new MemoryCommand());
        registry.register(new DoctorCommand());
        registry.register(new PermissionsCommand());
        registry.register(new StatusCommand());

        // P2 commands
        registry.register(new BranchCommand());
        registry.register(new EnvCommand());
        registry.register(new SkillsCommand());
        registry.register(new StatsCommand());
        registry.register(new InitCommand());
        registry.register(new HooksCommand());
        registry.register(new ThemeCommand());
        registry.register(new SearchCommand());
        registry.register(new LoginCommand());
        registry.register(new LogoutCommand());
        registry.register(new CopyCommand());
        registry.register(new FeedbackCommand());
        registry.register(new McpCommand());

        return registry;
    }
}
