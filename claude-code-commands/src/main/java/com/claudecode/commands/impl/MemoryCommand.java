package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MemoryCommand implements Command {

    private static final String MEMORY_DIR = ".claude/memories";

    @Override
    public String name() { return "memory"; }

    @Override
    public String description() { return "Manage memory entries"; }

    @Override
    public List<String> aliases() { return List.of("memories", "recall"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        Path memoryDir = Path.of(context.workingDirectory(), MEMORY_DIR);

        String action = args != null && !args.isBlank() ? args.trim().toLowerCase() : "list";

        if (action.equals("list") || action.equals("ls")) {
            return listMemories(memoryDir, sb);
        } else if (action.equals("add") || action.startsWith("add ")) {
            String content = action.startsWith("add ") ? action.substring(4) : "";
            return addMemory(memoryDir, sb, content);
        } else if (action.equals("clear")) {
            return clearMemories(memoryDir, sb);
        } else {
            sb.append("Memory Management\n");
            sb.append("=================\n\n");
            sb.append("Memories are stored in: ").append(memoryDir).append("\n\n");
            sb.append("Usage:\n");
            sb.append("  /memory list     - List all memories\n");
            sb.append("  /memory add <text> - Add a new memory\n");
            sb.append("  /memory clear    - Clear all memories\n");
            return CommandResult.of(sb.toString());
        }
    }

    private CommandResult listMemories(Path memoryDir, StringBuilder sb) {
        sb.append("Memory Entries\n");
        sb.append("==============\n\n");

        if (!memoryDir.toFile().exists()) {
            sb.append("No memories stored yet.\n");
            sb.append("Use /memory add <text> to add a memory.");
            return CommandResult.of(sb.toString());
        }

        try {
            var files = memoryDir.toFile().listFiles((dir, name) -> name.endsWith(".md"));
            if (files == null || files.length == 0) {
                sb.append("No memories stored yet.\n");
                sb.append("Use /memory add <text> to add a memory.");
                return CommandResult.of(sb.toString());
            }

            sb.append("Found ").append(files.length).append(" memory entries:\n\n");
            for (var file : files) {
                String name = file.getName().replace(".md", "");
                String content = Files.readString(file.toPath());
                String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                sb.append("[").append(name).append("]\n");
                sb.append(preview).append("\n\n");
            }
        } catch (IOException e) {
            sb.append("Failed to read memories: ").append(e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult addMemory(Path memoryDir, StringBuilder sb, String content) {
        if (content.isBlank()) {
            return CommandResult.of("Usage: /memory add <text>\nExample: /memory add Remember that the auth service uses JWT tokens");
        }

        try {
            if (!memoryDir.toFile().exists()) {
                Files.createDirectories(memoryDir);
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            Path memoryFile = memoryDir.resolve(timestamp + ".md");
            Files.writeString(memoryFile, content);

            sb.append("Memory added successfully!\n");
            sb.append("Saved to: ").append(memoryFile.getFileName());
        } catch (IOException e) {
            return CommandResult.of("Failed to add memory: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult clearMemories(Path memoryDir, StringBuilder sb) {
        if (!memoryDir.toFile().exists()) {
            return CommandResult.of("No memories to clear.");
        }

        try {
            var files = memoryDir.toFile().listFiles((dir, name) -> name.endsWith(".md"));
            if (files != null) {
                for (var file : files) {
                    Files.delete(file.toPath());
                }
            }
            sb.append("All memories cleared.");
        } catch (IOException e) {
            return CommandResult.of("Failed to clear memories: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }
}
