package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;
import com.claudecode.core.message.Message;
import com.claudecode.core.message.Usage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StatusCommand implements Command {

    @Override
    public String name() { return "status"; }

    @Override
    public String description() { return "Show current session status"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Status\n");
        sb.append("==============\n\n");

        sb.append("Session: active\n");
        sb.append("Model: ").append(context.model()).append("\n");
        sb.append("Working directory: ").append(context.workingDirectory()).append("\n");
        sb.append("Remote mode: ").append(context.remoteMode() ? "enabled" : "disabled").append("\n\n");

        List<Message> messages = context.messagesSupplier().get();
        int messageCount = messages.size();
        sb.append("Messages in session: ").append(messageCount).append("\n\n");

        Usage usage = context.usageSupplier().get();
        if (usage != null && usage != Usage.EMPTY) {
            sb.append("Token Usage:\n");
            sb.append("  Input tokens: ").append(usage.inputTokens()).append("\n");
            sb.append("  Output tokens: ").append(usage.outputTokens()).append("\n");
            sb.append("  Total tokens: ").append(usage.totalTokens()).append("\n\n");

            double cost = context.costCalculator().applyAsDouble(usage);
            sb.append("Estimated cost: $").append(String.format("%.6f", cost)).append("\n\n");
        }

        var timestamp = messages.isEmpty() ? Instant.now() : 
            messages.get(messages.size() - 1).timestamp().orElse(Instant.now());
        String formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(timestamp);
        sb.append("Last activity: ").append(formattedTime);

        return CommandResult.of(sb.toString());
    }
}
