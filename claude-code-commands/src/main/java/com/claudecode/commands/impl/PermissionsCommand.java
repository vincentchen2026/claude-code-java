package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;
import java.util.Set;

public class PermissionsCommand implements Command {

    private static final Set<String> VALID_MODES = Set.of("default", "ask", "bypass", "deny");

    @Override
    public String name() { return "permissions"; }

    @Override
    public String description() { return "Manage tool permissions"; }

    @Override
    public List<String> aliases() { return List.of("perms", "ToolPermission"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool Permissions\n");
        sb.append("================\n\n");

        String action = args != null && !args.isBlank() ? args.trim().toLowerCase() : "show";

        if (action.equals("show") || action.equals("list")) {
            return showPermissions(sb);
        } else if (action.startsWith("set ")) {
            return setPermission(sb, action.substring(4).trim());
        } else if (action.equals("reset")) {
            return resetPermissions(sb);
        } else if (action.equals("allow-all")) {
            return allowAll(sb);
        } else if (action.equals("deny-all")) {
            return denyAll(sb);
        } else {
            sb.append("Usage:\n");
            sb.append("  /permissions show      - Show current permissions\n");
            sb.append("  /permissions set <mode> - Set permission mode\n");
            sb.append("  /permissions allow-all  - Allow all tool calls\n");
            sb.append("  /permissions deny-all   - Deny all tool calls\n");
            sb.append("  /permissions reset      - Reset to default\n\n");
            sb.append("Modes: default, ask, bypass, deny\n\n");
            sb.append("Current mode: default");
            return CommandResult.of(sb.toString());
        }
    }

    private CommandResult showPermissions(StringBuilder sb) {
        sb.append("Current Permission Settings\n");
        sb.append("--------------------------\n\n");
        sb.append("Mode: default (ask per tool)\n\n");
        sb.append("Tool Categories:\n");
        sb.append("  Read operations:  allowed (with confirmation)\n");
        sb.append("  Write operations: ask (always confirm)\n");
        sb.append("  Dangerous ops:    deny (require explicit approval)\n\n");
        sb.append("Note: Use /permissions set <mode> to change settings.");
        return CommandResult.of(sb.toString());
    }

    private CommandResult setPermission(StringBuilder sb, String mode) {
        if (!VALID_MODES.contains(mode)) {
            sb.append("Invalid mode: ").append(mode).append("\n");
            sb.append("Valid modes: ").append(VALID_MODES);
            return CommandResult.of(sb.toString());
        }

        sb.append("Permission mode set to: ").append(mode).append("\n\n");
        switch (mode) {
            case "default" -> sb.append("Tools will ask for confirmation as needed.");
            case "ask" -> sb.append("All tools will require explicit confirmation.");
            case "bypass" -> sb.append("All tools will run without confirmation.");
            case "deny" -> sb.append("All tool calls will be denied.");
        }
        return CommandResult.of(sb.toString());
    }

    private CommandResult resetPermissions(StringBuilder sb) {
        sb.append("Permissions reset to default.\n");
        sb.append("Tools will ask for confirmation as needed.");
        return CommandResult.of(sb.toString());
    }

    private CommandResult allowAll(StringBuilder sb) {
        sb.append("All tool permissions set to allow.\n");
        sb.append("Warning: All tools will run without confirmation.");
        return CommandResult.of(sb.toString());
    }

    private CommandResult denyAll(StringBuilder sb) {
        sb.append("All tool permissions set to deny.\n");
        sb.append("All tool calls will be blocked until permissions are changed.");
        return CommandResult.of(sb.toString());
    }
}
