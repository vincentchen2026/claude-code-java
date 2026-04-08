package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class LoginCommand implements Command {

    @Override
    public String name() { return "login"; }

    @Override
    public String description() { return "Authenticate with API provider"; }

    @Override
    public List<String> aliases() { return List.of(); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Authentication\n");
        sb.append("=============\n\n");

        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            sb.append("Already authenticated via ANTHROPIC_API_KEY environment variable.\n");
        } else {
            sb.append("Not currently authenticated.\n\n");
            sb.append("Options for authentication:\n\n");
            sb.append("1. Set API key environment variable:\n");
            sb.append("   export ANTHROPIC_API_KEY=your-api-key\n\n");
            sb.append("2. Use --api-key flag when starting Claude Code:\n");
            sb.append("   claude --api-key your-api-key\n\n");
            sb.append("3. Configure in ~/.claude/settings.json:\n");
            sb.append("   {\n");
            sb.append("     \"apiKey\": \"your-api-key\"\n");
            sb.append("   }\n\n");
            sb.append("Get your API key from: https://console.anthropic.com/\n");
        }

        return CommandResult.of(sb.toString());
    }
}