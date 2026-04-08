package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BranchCommand implements Command {

    @Override
    public String name() { return "branch"; }

    @Override
    public String description() { return "Show or switch git branches"; }

    @Override
    public List<String> aliases() { return List.of("git-branch"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        String workingDir = context.workingDirectory();

        if (args == null || args.isBlank()) {
            return showBranches(sb, workingDir);
        }

        String[] parts = args.trim().split("\\s+", 2);
        String action = parts[0].toLowerCase();
        String branchName = parts.length > 1 ? parts[1].trim() : "";

        return switch (action) {
            case "list", "ls" -> showBranches(sb, workingDir);
            case "current", "show" -> showCurrentBranch(sb, workingDir);
            case "create", "new" -> createBranch(sb, workingDir, branchName);
            case "switch", "checkout" -> switchBranch(sb, workingDir, branchName);
            case "delete", "rm" -> deleteBranch(sb, workingDir, branchName);
            case "-d", "-D" -> forceDeleteBranch(sb, workingDir, branchName);
            case "-m", "rename" -> {
                if (parts.length < 2) {
                    yield CommandResult.of("Usage: /branch -m <oldname> <newname>");
                }
                String[] nameParts = branchName.split("\\s+", 2);
                if (nameParts.length < 2) {
                    yield CommandResult.of("Usage: /branch -m <oldname> <newname>");
                }
                yield renameBranch(sb, workingDir, nameParts[0], nameParts[1]);
            }
            default -> {
                if (action.startsWith("-")) {
                    yield CommandResult.of("Unknown branch option: " + action);
                }
                yield switchBranch(sb, workingDir, args.trim());
            }
        };
    }

    private CommandResult showBranches(StringBuilder sb, String workingDir) {
        sb.append("Git Branches\n");
        sb.append("============\n\n");

        try {
            String current = runGitCommand(workingDir, "git", "branch", "--show-current");
            String branches = runGitCommand(workingDir, "git", "branch", "-a");

            if (branches.isBlank()) {
                sb.append("No branches found.");
                return CommandResult.of(sb.toString());
            }

            sb.append("Current branch: ").append(BOLD).append(current.isBlank() ? "(detached)" : current).append(RESET).append("\n\n");
            sb.append("Local branches:\n");

            for (String line : branches.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("*")) {
                    trimmed = trimmed.substring(1).trim();
                    sb.append("  * ").append(trimmed).append(" (current)\n");
                } else if (!trimmed.startsWith("remotes/")) {
                    sb.append("    ").append(trimmed).append("\n");
                }
            }

            sb.append("\nRemote branches:\n");
            for (String line : branches.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("remotes/")) {
                    sb.append("    ").append(trimmed).append("\n");
                }
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to list branches: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult showCurrentBranch(StringBuilder sb, String workingDir) {
        try {
            String current = runGitCommand(workingDir, "git", "branch", "--show-current");
            sb.append("Current branch: ").append(BOLD).append(current.isBlank() ? "(detached HEAD)" : current).append(RESET);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to get current branch: " + e.getMessage());
        }
        return CommandResult.of(sb.toString());
    }

    private CommandResult createBranch(StringBuilder sb, String workingDir, String branchName) {
        if (branchName.isBlank()) {
            return CommandResult.of("Usage: /branch create <branchname>");
        }

        try {
            runGitCommand(workingDir, "git", "branch", branchName);
            sb.append("Created branch: ").append(BOLD).append(branchName).append(RESET);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to create branch: " + e.getMessage());
        }
        return CommandResult.of(sb.toString());
    }

    private CommandResult switchBranch(StringBuilder sb, String workingDir, String branchName) {
        if (branchName.isBlank()) {
            return CommandResult.of("Usage: /branch switch <branchname>");
        }

        try {
            String result = runGitCommand(workingDir, "git", "checkout", branchName);
            sb.append("Switched to branch: ").append(BOLD).append(branchName).append(RESET);
            if (!result.isBlank() && !result.contains("Switched")) {
                sb.append("\n").append(result);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to switch branch: " + e.getMessage());
        }
        return CommandResult.of(sb.toString());
    }

    private CommandResult deleteBranch(StringBuilder sb, String workingDir, String branchName) {
        if (branchName.isBlank()) {
            return CommandResult.of("Usage: /branch delete <branchname>");
        }

        try {
            runGitCommand(workingDir, "git", "branch", "-d", branchName);
            sb.append("Deleted branch: ").append(branchName);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to delete branch: " + e.getMessage());
        }
        return CommandResult.of(sb.toString());
    }

    private CommandResult forceDeleteBranch(StringBuilder sb, String workingDir, String branchName) {
        if (branchName.isBlank()) {
            return CommandResult.of("Usage: /branch -D <branchname>");
        }

        try {
            runGitCommand(workingDir, "git", "branch", "-D", branchName);
            sb.append("Force deleted branch: ").append(branchName);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to force delete branch: " + e.getMessage());
        }
        return CommandResult.of(sb.toString());
    }

    private CommandResult renameBranch(StringBuilder sb, String workingDir, String oldName, String newName) {
        if (oldName.isBlank() || newName.isBlank()) {
            return CommandResult.of("Usage: /branch -m <oldname> <newname>");
        }

        try {
            runGitCommand(workingDir, "git", "branch", "-m", oldName, newName);
            sb.append("Renamed branch: ").append(oldName).append(" -> ").append(newName);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to rename branch: " + e.getMessage());
        }
        return CommandResult.of(sb.toString());
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

    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";
}
