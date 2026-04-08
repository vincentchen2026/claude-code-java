package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class HooksCommand implements Command {

    @Override
    public String name() { return "hooks"; }

    @Override
    public String description() { return "Manage lifecycle hooks"; }

    @Override
    public List<String> aliases() { return List.of("hook"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        String action = args != null && !args.isBlank() ? args.trim().toLowerCase() : "list";

        return switch (action) {
            case "list", "ls" -> listHooks(sb);
            case "events" -> listEvents(sb);
            case "help" -> showHelp(sb);
            default -> showHelp(sb);
        };
    }

    private CommandResult showHelp(StringBuilder sb) {
        sb.append("Lifecycle Hooks Management\n");
        sb.append("===========================\n\n");
        sb.append("Hooks run custom scripts at various points during Claude Code execution.\n\n");
        sb.append("Commands:\n");
        sb.append("  /hooks list     - List all configured hooks\n");
        sb.append("  /hooks events   - Show available hook events\n\n");
        sb.append("Hooks are configured in ~/.claude/settings.json under the 'hooks' section.\n");
        sb.append("See https://docs.claude.ai/hooks for configuration details.\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult listHooks(StringBuilder sb) {
        sb.append("Configured Hooks\n");
        sb.append("================\n\n");
        sb.append("Hooks are configured in settings.json.\n\n");
        sb.append("Run /hooks events to see available hook types.\n\n");
        sb.append("Example settings.json structure:\n");
        sb.append("{\n");
        sb.append("  \"hooks\": {\n");
        sb.append("    \"AfterMessage\": [{\n");
        sb.append("      \"matcher\": \"error.*\",\n");
        sb.append("      \"hooks\": [{\n");
        sb.append("        \"type\": \"command\",\n");
        sb.append("        \"command\": \"echo 'Error occurred'\",\n");
        sb.append("        \"once\": false\n");
        sb.append("      }]\n");
        sb.append("    }]\n");
        sb.append("  }\n");
        sb.append("}\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult listEvents(StringBuilder sb) {
        sb.append("Available Hook Events\n");
        sb.append("=====================\n\n");
        sb.append("PreToolUse        - Before a tool is executed\n");
        sb.append("PostToolUse       - After a tool completes\n");
        sb.append("BeforeMessage     - Before sending a message to the LLM\n");
        sb.append("AfterMessage      - After receiving a response\n");
        sb.append("OnStartup         - When Claude Code starts\n");
        sb.append("OnExit            - When Claude Code exits\n");
        sb.append("OnError           - When an error occurs\n");
        sb.append("OnCompact         - Before/after conversation compact\n");
        sb.append("OnToolUseBlocked  - When a tool is blocked\n\n");
        sb.append("Hook types: command, prompt, http, agent\n");
        return CommandResult.of(sb.toString());
    }
}