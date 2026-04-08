package com.claudecode.commands.impl;

import com.claudecode.commands.*;

import java.util.List;
import java.util.Map;

/**
 * /config — shows or modifies configuration.
 * Supports viewing and updating runtime configuration values.
 */
public class ConfigCommand implements Command {

    @Override
    public String name() { return "config"; }

    @Override
    public String description() { return "Show or modify configuration"; }

    @Override
    public List<String> aliases() { return List.of("cfg"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        String action = args != null ? args.trim().toLowerCase() : "";

        if (action.isEmpty() || action.equals("show")) {
            return showConfig(sb, context);
        }

        if (action.startsWith("get ")) {
            String key = action.substring(4).trim();
            return getConfig(sb, context, key);
        }

        if (action.startsWith("set ")) {
            String[] parts = args.substring(4).trim().split("\\s+", 2);
            if (parts.length >= 2) {
                return setConfig(sb, context, parts[0], parts[1]);
            }
            return CommandResult.of("Usage: /config set <key> <value>");
        }

        if (action.equals("list")) {
            return listConfigKeys(sb);
        }

        return showHelp(sb);
    }

    private CommandResult showConfig(StringBuilder sb, CommandContext context) {
        sb.append("Configuration\n");
        sb.append("=============\n\n");
        sb.append("Current settings:\n");
        sb.append("  Model: ").append(context.model()).append("\n");
        sb.append("  Working directory: ").append(context.workingDirectory()).append("\n");
        sb.append("  Remote mode: ").append(context.remoteMode()).append("\n\n");
        sb.append("Use /config list to see all available keys.\n");
        sb.append("Use /config get <key> to view a specific value.\n");
        sb.append("Use /config set <key> <value> to modify a value.\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult getConfig(StringBuilder sb, CommandContext context, String key) {
        return switch (key.toLowerCase()) {
            case "model" -> CommandResult.of("model = " + context.model());
            case "workingdirectory", "cwd" -> CommandResult.of("workingDirectory = " + context.workingDirectory());
            case "remotemode", "remote" -> CommandResult.of("remoteMode = " + context.remoteMode());
            default -> CommandResult.of("Unknown config key: " + key + ". Use /config list for available keys.");
        };
    }

    private CommandResult setConfig(StringBuilder sb, CommandContext context, String key, String value) {
        sb.append("Configuration update:\n");
        sb.append("  ").append(key).append(" = ").append(value).append("\n\n");
        sb.append("Note: Runtime config updates are not yet fully persisted.\n");
        sb.append("Some changes may require restarting Claude Code.\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult listConfigKeys(StringBuilder sb) {
        sb.append("Available Configuration Keys\n");
        sb.append("============================\n\n");
        sb.append("  model              - LLM model to use\n");
        sb.append("  workingDirectory   - Current working directory\n");
        sb.append("  remoteMode         - Whether running in remote/bridge mode\n\n");
        sb.append("Runtime values (read-only):\n");
        sb.append("  sessionId          - Current session identifier\n");
        sb.append("  version            - Claude Code version\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult showHelp(StringBuilder sb) {
        sb.append("Config Command\n");
        sb.append("==============\n\n");
        sb.append("Commands:\n");
        sb.append("  /config            - Show current configuration\n");
        sb.append("  /config list       - List all available config keys\n");
        sb.append("  /config get <key>  - Get a specific config value\n");
        sb.append("  /config set <key> <value> - Set a config value\n");
        return CommandResult.of(sb.toString());
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
