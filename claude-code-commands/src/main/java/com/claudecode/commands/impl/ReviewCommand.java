package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReviewCommand implements Command {

    @Override
    public String name() { return "review"; }

    @Override
    public String description() { return "Review code changes"; }

    @Override
    public List<String> aliases() { return List.of("code-review"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        String workingDir = context.workingDirectory();

        sb.append("Code Review Mode\n");
        sb.append("================\n\n");

        try {
            String untracked = runGitCommand(workingDir, "git", "ls-files", "--others", "--exclude-standard");
            String modified = runGitCommand(workingDir, "git", "diff", "--name-only");
            String staged = runGitCommand(workingDir, "git", "diff", "--cached", "--name-only");

            boolean hasChanges = !untracked.isBlank() || !modified.isBlank() || !staged.isBlank();

            if (!hasChanges) {
                sb.append("No code changes to review. Working directory is clean.");
                return CommandResult.of(sb.toString());
            }

            if (!staged.isBlank()) {
                sb.append("Staged changes (ready to commit):\n");
                for (String file : staged.split("\n")) {
                    sb.append("  [staged] ").append(file).append("\n");
                }
                sb.append("\n");
            }

            if (!modified.isBlank()) {
                sb.append("Unstaged changes:\n");
                for (String file : modified.split("\n")) {
                    sb.append("  [modified] ").append(file).append("\n");
                }
                sb.append("\n");
            }

            if (!untracked.isBlank()) {
                sb.append("Untracked files:\n");
                for (String file : untracked.split("\n")) {
                    sb.append("  [new] ").append(file).append("\n");
                }
                sb.append("\n");
            }

            sb.append("To review specific changes, use:\n");
            sb.append("  /diff          - review unstaged changes\n");
            sb.append("  /diff --staged - review staged changes\n");
            sb.append("  /diff --no-stat - review without detailed diff\n\n");
            sb.append("Or send a message asking me to review specific files or the changes above.");

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.of("Failed to gather changes for review: " + e.getMessage());
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
}
