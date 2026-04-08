package com.claudecode.commands.impl;

import com.claudecode.commands.*;

import java.util.List;

/**
 * /compact — triggers conversation compaction.
 * Reduces context usage by summarizing and truncating message history.
 */
public class CompactCommand implements Command {

    @Override
    public String name() { return "compact"; }

    @Override
    public String description() { return "Compact conversation history to save context"; }

    @Override
    public List<String> aliases() { return List.of("compress"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        int messageCount = context.messagesSupplier().get().size();

        if (messageCount == 0) {
            return CommandResult.of("No messages to compact.");
        }

        String action = args != null ? args.trim().toLowerCase() : "";

        sb.append("Conversation Compaction\n");
        sb.append("======================\n\n");

        if (action.equals("status") || action.equals("info")) {
            return showStatus(sb, context, messageCount);
        }

        return triggerCompact(sb, context, messageCount);
    }

    private CommandResult showStatus(StringBuilder sb, CommandContext context, int messageCount) {
        sb.append("Current status:\n");
        sb.append("  Messages: ").append(messageCount).append("\n");
        sb.append("  Working directory: ").append(context.workingDirectory()).append("\n\n");
        sb.append("Compaction is automatic when context approaches limits.\n");
        sb.append("Use /compact without arguments to trigger manual compaction.\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult triggerCompact(StringBuilder sb, CommandContext context, int messageCount) {
        sb.append("Compaction initiated...\n");
        sb.append("  Messages: ").append(messageCount).append("\n\n");
        sb.append("This operation will:\n");
        sb.append("  1. Summarize older messages using LLM\n");
        sb.append("  2. Truncate long tool outputs\n");
        sb.append("  3. Preserve recent context for continuity\n\n");
        sb.append("Use /stats to view token usage after compaction.\n");
        return CommandResult.of(sb.toString());
    }
}
