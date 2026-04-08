package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class ShareCommand implements Command {

    @Override
    public String name() { return "share"; }

    @Override
    public String description() { return "Share the current conversation"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Share Conversation\n");
        sb.append("==================\n\n");

        sb.append("Session ID: ").append(getSessionId(context)).append("\n\n");

        sb.append("Share options:\n\n");

        sb.append("1. Export and share:\n");
        sb.append("   /export <file.json>\n");
        sb.append("   Then share the exported file manually.\n\n");

        sb.append("2. Session link (if sharing service is configured):\n");
        sb.append("   Use --share flag when starting claude-code to enable session sharing.\n\n");

        sb.append("3. Terminal recording:\n");
        sb.append("   Use a tool like 'script' or 'asciinema' to record your terminal session.\n\n");

        sb.append("Note: Sharing is read-only. The recipient can view but not modify the session.");

        return CommandResult.of(sb.toString());
    }

    private String getSessionId(CommandContext context) {
        var messages = context.messagesSupplier().get();
        if (messages.isEmpty()) {
            return "no-active-session";
        }
        return "session-" + messages.hashCode();
    }
}
