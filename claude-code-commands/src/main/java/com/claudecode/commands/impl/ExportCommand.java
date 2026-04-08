package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;
import com.claudecode.core.message.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportCommand implements Command {

    @Override
    public String name() { return "export"; }

    @Override
    public String description() { return "Export conversation to a file"; }

    @Override
    public List<String> aliases() { return List.of("save", "dump"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        List<Message> messages = context.messagesSupplier().get();

        if (messages.isEmpty()) {
            return CommandResult.of("No conversation to export.");
        }

        String filePath = args;
        if (filePath == null || filePath.isBlank()) {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(java.time.ZoneId.systemDefault())
                .format(Instant.now());
            filePath = "conversation-" + timestamp + ".json";
        }

        try {
            Path path = Path.of(filePath);
            if (path.getParent() != null && !path.getParent().toFile().exists()) {
                Files.createDirectories(path.getParent());
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"exported\": \"").append(Instant.now()).append("\",\n");
            json.append("  \"model\": \"").append(context.model()).append("\",\n");
            json.append("  \"messageCount\": ").append(messages.size()).append(",\n");
            json.append("  \"messages\": [\n");

            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                json.append("    {\n");
                json.append("      \"type\": \"").append(msg.type()).append("\",\n");
                json.append("      \"uuid\": \"").append(msg.uuid()).append("\",\n");
                json.append("      \"content\": ").append(escapeJson(msg.toString())).append("\n");
                json.append("    }");
                if (i < messages.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}\n");

            Files.writeString(path, json.toString());
            sb.append("Conversation exported to: ").append(path.toAbsolutePath());
            sb.append("\n(").append(messages.size()).append(" messages)");

        } catch (IOException e) {
            return CommandResult.of("Failed to export conversation: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\"";
    }
}
