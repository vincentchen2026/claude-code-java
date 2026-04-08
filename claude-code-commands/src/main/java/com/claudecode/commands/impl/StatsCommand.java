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

public class StatsCommand implements Command {

    @Override
    public String name() { return "stats"; }

    @Override
    public String description() { return "Show session statistics"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session Statistics\n");
        sb.append("==================\n\n");

        List<Message> messages = context.messagesSupplier().get();
        int messageCount = messages.size();

        sb.append("Messages:\n");
        sb.append("  Total messages: ").append(messageCount).append("\n");
        
        long userMessages = messages.stream()
            .filter(m -> m.type().equals("user"))
            .count();
        long assistantMessages = messages.stream()
            .filter(m -> m.type().equals("assistant"))
            .count();
        
        sb.append("  User messages: ").append(userMessages).append("\n");
        sb.append("  Assistant messages: ").append(assistantMessages).append("\n\n");

        Usage usage = context.usageSupplier().get();
        if (usage != null && usage != Usage.EMPTY) {
            sb.append("Token Usage:\n");
            sb.append("  Input tokens: ").append(formatNumber(usage.inputTokens())).append("\n");
            sb.append("  Output tokens: ").append(formatNumber(usage.outputTokens())).append("\n");
            sb.append("  Total tokens: ").append(formatNumber(usage.totalTokens())).append("\n\n");

            double cost = context.costCalculator().applyAsDouble(usage);
            sb.append("Cost:\n");
            sb.append("  Estimated: $").append(String.format("%.6f", cost)).append("\n\n");
        }

        long totalCharacters = messages.stream()
            .mapToLong(m -> m.toString().length())
            .sum();
        sb.append("Content:\n");
        sb.append("  Total characters: ").append(formatNumber(totalCharacters)).append("\n");
        sb.append("  Avg message size: ").append(messageCount > 0 ? formatNumber(totalCharacters / messageCount) : 0).append(" chars\n\n");

        if (!messages.isEmpty()) {
            Instant firstTime = messages.get(0).timestamp().orElse(Instant.now());
            Instant lastTime = messages.get(messages.size() - 1).timestamp().orElse(Instant.now());
            long durationSeconds = lastTime.getEpochSecond() - firstTime.getEpochSecond();
            
            sb.append("Duration:\n");
            sb.append("  Session start: ").append(formatTime(firstTime)).append("\n");
            sb.append("  Last activity: ").append(formatTime(lastTime)).append("\n");
            sb.append("  Session length: ").append(formatDuration(durationSeconds)).append("\n\n");
        }

        sb.append("Model: ").append(context.model()).append("\n");
        sb.append("Working directory: ").append(context.workingDirectory()).append("\n");
        sb.append("Remote mode: ").append(context.remoteMode() ? "enabled" : "disabled");

        return CommandResult.of(sb.toString());
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fk", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    private String formatTime(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(instant);
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long mins = seconds / 60;
            long secs = seconds % 60;
            return mins + "m " + secs + "s";
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            return hours + "h " + mins + "m";
        }
    }
}
