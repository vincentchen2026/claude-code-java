package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommitCommand implements Command {

    @Override
    public String name() { return "commit"; }

    @Override
    public String description() { return "Create a git commit"; }

    @Override
    public List<String> aliases() { return List.of("git-commit"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();

        if (!isGitRepo(context.workingDirectory())) {
            return CommandResult.of("Not a git repository. Cannot commit.");
        }

        try {
            String status = runGitCommand(context.workingDirectory(), "git", "status", "--porcelain");
            if (status.isBlank()) {
                return CommandResult.of("Nothing to commit. Working directory is clean.");
            }

            String stagedStatus = runGitCommand(context.workingDirectory(), "git", "diff", "--cached", "--stat");
            if (stagedStatus.isBlank()) {
                // No staged changes - auto stage all changes (modified and untracked files)
                String addResult = runGitCommand(context.workingDirectory(), "git", "add", "-A");
                stagedStatus = runGitCommand(context.workingDirectory(), "git", "diff", "--cached", "--stat");
                if (stagedStatus.isBlank()) {
                    return CommandResult.of("Nothing to commit. Working directory is clean.");
                }
                sb.append("Auto-staged all changes:\n").append(stagedStatus).append("\n\n");
            }

            if (args == null || args.isBlank()) {
                sb.append("Staged changes:\n").append(stagedStatus);
                sb.append("\nCommit message not provided.\n");
                sb.append("Usage: /commit <message>\n");
                sb.append("Example: /commit Add user authentication feature");
                return CommandResult.of(sb.toString());
            }

            String commitResult = runGitCommand(context.workingDirectory(), "git", "commit", "-m", args);
            sb.append("Commit created successfully:\n").append(commitResult);

            String logResult = runGitCommand(context.workingDirectory(), "git", "log", "-1", "--format=%H%n%an%n%ae%n%at%n%s");
            if (!logResult.isBlank()) {
                String[] parts = logResult.split("\n", 5);
                if (parts.length >= 4) {
                    sb.append("\nCommit details:\n");
                    sb.append("  Hash: ").append(parts[0].substring(0, 7)).append("\n");
                    sb.append("  Author: ").append(parts[1]).append("\n");
                    sb.append("  Date: ").append(formatTimestamp(parts[3])).append("\n");
                    sb.append("  Message: ").append(parts[4]);
                }
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to create commit: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private boolean isGitRepo(String workingDir) {
        try {
            runGitCommand(workingDir, "git", "rev-parse", "--is-inside-work-tree");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String runGitCommand(String workingDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(Path.of(workingDir).toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.destroyForcibly();
        }
        return output.trim();
    }

    private String formatTimestamp(String epochStr) {
        try {
            long epoch = Long.parseLong(epochStr);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault())
                .format(Instant.ofEpochSecond(epoch));
        } catch (Exception e) {
            return epochStr;
        }
    }
}
