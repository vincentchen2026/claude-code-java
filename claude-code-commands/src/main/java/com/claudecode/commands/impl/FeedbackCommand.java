package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class FeedbackCommand implements Command {

    @Override
    public String name() { return "feedback"; }

    @Override
    public String description() { return "Send feedback to the team"; }

    @Override
    public List<String> aliases() { return List.of("suggest"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();

        if (args == null || args.isBlank()) {
            return showHelp(sb);
        }

        sb.append("Feedback Submitted\n");
        sb.append("==================\n\n");
        sb.append("Thank you for your feedback!\n\n");
        sb.append("Your feedback: \"").append(args).append("\"\n\n");
        sb.append("This feature is not yet fully implemented.\n");
        sb.append("For now, please send feedback via:\n");
        sb.append("  https://github.com/anthropics/claude-code/issues\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult showHelp(StringBuilder sb) {
        sb.append("Send Feedback\n");
        sb.append("=============\n\n");
        sb.append("Usage: /feedback <your feedback message>\n\n");
        sb.append("Examples:\n");
        sb.append("  /feedback The new theme feature is great!\n");
        sb.append("  /feedback Would be nice to have git integration\n\n");
        sb.append("For issues and feature requests, visit:\n");
        sb.append("  https://github.com/anthropics/claude-code/issues\n");
        return CommandResult.of(sb.toString());
    }
}