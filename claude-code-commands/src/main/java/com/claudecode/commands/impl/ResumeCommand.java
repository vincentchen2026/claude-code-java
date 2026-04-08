package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.nio.file.Path;
import java.util.List;

public class ResumeCommand implements Command {

    @Override
    public String name() { return "resume"; }

    @Override
    public String description() { return "Resume a previous session"; }

    @Override
    public List<String> aliases() { return List.of("restore"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Resume\n");
        sb.append("=============\n\n");

        Path sessionDir = Path.of(System.getProperty("user.home"), ".claude", "sessions");
        if (!sessionDir.toFile().exists()) {
            sb.append("No previous sessions found.\n");
            sb.append("Sessions are stored in: ").append(sessionDir);
            return CommandResult.of(sb.toString());
        }

        String[] sessionFiles = sessionDir.toFile().list((dir, name) -> name.endsWith(".json"));
        if (sessionFiles == null || sessionFiles.length == 0) {
            sb.append("No previous sessions found.\n");
            sb.append("Sessions are stored in: ").append(sessionDir);
            return CommandResult.of(sb.toString());
        }

        sb.append("Previous sessions:\n");
        for (String file : sessionFiles) {
            sb.append("  - ").append(file.replace(".json", "")).append("\n");
        }

        sb.append("\nTo resume a session, use:\n");
        sb.append("  claude-code --resume <session-id>\n\n");
        sb.append("Or start a new session and use /import to import a previous session.");

        return CommandResult.of(sb.toString());
    }
}
