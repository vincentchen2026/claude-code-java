package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class LogoutCommand implements Command {

    @Override
    public String name() { return "logout"; }

    @Override
    public String description() { return "Clear authentication credentials"; }

    @Override
    public List<String> aliases() { return List.of("signout"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Logout\n");
        sb.append("======\n\n");

        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            sb.append("Not currently authenticated.\n");
        } else {
            sb.append("To logout, unset your API key:\n\n");
            sb.append("  unset ANTHROPIC_API_KEY\n\n");
            sb.append("Or remove the apiKey from ~/.claude/settings.json\n");
            sb.append("Restart Claude Code after making changes.\n");
        }

        return CommandResult.of(sb.toString());
    }
}