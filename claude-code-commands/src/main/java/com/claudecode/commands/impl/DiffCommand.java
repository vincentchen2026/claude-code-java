package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiffCommand implements Command {

    @Override
    public String name() { return "diff"; }

    @Override
    public String description() { return "Show git diff summary"; }

    @Override
    public List<String> aliases() { return List.of("git-diff"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        String workingDir = context.workingDirectory();

        boolean showStat = args == null || !args.contains("--no-stat");
        boolean staged = args != null && args.contains("--staged");

        try {
            if (staged) {
                String stagedFiles = runGitCommand(workingDir, "git", "diff", "--cached", "--name-only");
                if (stagedFiles.isBlank()) {
                    return CommandResult.of("No staged changes.");
                }

                sb.append("Staged changes:\n");
                String diffStat = runGitCommand(workingDir, "git", "diff", "--cached", "--stat");
                sb.append(diffStat).append("\n\nFiles:\n").append(stagedFiles);

                if (showStat) {
                    String diff = runGitCommand(workingDir, "git", "diff", "--cached");
                    if (!diff.isBlank()) {
                        sb.append("\n\nDetailed diff:\n").append(truncateDiff(diff, 100));
                    }
                }
            } else {
                String changedFiles = runGitCommand(workingDir, "git", "diff", "--name-only");
                if (changedFiles.isBlank()) {
                    String stagedFiles = runGitCommand(workingDir, "git", "diff", "--cached", "--name-only");
                    if (stagedFiles.isBlank()) {
                        return CommandResult.of("No uncommitted changes.");
                    }
                    sb.append("No unstaged changes. Use /diff --staged to see staged changes.");
                    return CommandResult.of(sb.toString());
                }

                sb.append("Changed files:\n");
                String diffStat = runGitCommand(workingDir, "git", "diff", "--stat");
                sb.append(diffStat).append("\n\nFiles:\n").append(changedFiles);

                if (showStat) {
                    String diff = runGitCommand(workingDir, "git", "diff");
                    if (!diff.isBlank()) {
                        sb.append("\n\nDetailed diff:\n").append(truncateDiff(diff, 100));
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to run git diff: " + e.getMessage());
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

    private String truncateDiff(String diff, int maxLines) {
        String[] lines = diff.split("\n");
        if (lines.length <= maxLines) {
            return diff;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("... (").append(lines.length - maxLines).append(" more lines)");
        return sb.toString();
    }
}
