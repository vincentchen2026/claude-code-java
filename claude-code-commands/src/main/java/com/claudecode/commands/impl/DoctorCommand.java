package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class DoctorCommand implements Command {

    @Override
    public String name() { return "doctor"; }

    @Override
    public String description() { return "Run diagnostic checks"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostics\n");
        sb.append("===========\n\n");

        sb.append("Java: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("Java Home: ").append(System.getProperty("java.home")).append("\n\n");

        sb.append("OS: ").append(System.getProperty("os.name")).append(" ");
        sb.append(System.getProperty("os.version")).append(" ");
        sb.append(System.getProperty("os.arch")).append("\n\n");

        sb.append("Model: ").append(context.model()).append("\n");
        sb.append("Working directory: ").append(context.workingDirectory()).append("\n\n");

        checkGit(sb, context.workingDirectory());
        checkJavaTools(sb);
        checkNetwork(sb);

        return CommandResult.of(sb.toString());
    }

    private void checkGit(StringBuilder sb, String workingDir) {
        sb.append("Git:\n");
        try {
            String version = runCommand("git", "--version");
            sb.append("  Version: ").append(version).append("\n");

            String branch = runCommand("git", "branch", "--show-current");
            sb.append("  Branch: ").append(branch.isBlank() ? "(detached)" : branch).append("\n");

            String status = runCommand("git", "status", "--porcelain");
            if (status.isBlank()) {
                sb.append("  Status: clean\n");
            } else {
                int lines = status.split("\n").length;
                sb.append("  Status: ").append(lines).append(" file(s) changed\n");
            }
        } catch (Exception e) {
            sb.append("  Error: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n");
    }

    private void checkJavaTools(StringBuilder sb) {
        sb.append("Java Tools:\n");
        sb.append("  Java: ").append(System.getProperty("java.version")).append("\n");

        try {
            String mavenVersion = runCommand("mvn", "--version");
            String[] lines = mavenVersion.split("\n");
            if (lines.length > 0) {
                sb.append("  Maven: ").append(lines[0].split("Apache Maven ")[1]).append("\n");
            }
        } catch (Exception e) {
            sb.append("  Maven: not found\n");
        }

        try {
            String gradleVersion = runCommand("gradle", "--version");
            String[] lines = gradleVersion.split("\n");
            if (lines.length > 0) {
                sb.append("  Gradle: ").append(lines[0].replace("Gradle ", "").split(" ")[0]).append("\n");
            }
        } catch (Exception e) {
            sb.append("  Gradle: not found\n");
        }

        sb.append("\n");
    }

    private void checkNetwork(StringBuilder sb) {
        sb.append("Network:\n");

        try {
            String baseUrl = "https://api.minimaxi.com";
            long start = System.currentTimeMillis();
            runCommand("curl", "-s", "-o", "/dev/null", "-w", "%{http_code}", "--max-time", "5", baseUrl);
            long elapsed = System.currentTimeMillis() - start;
            sb.append("  API Endpoint: reachable (").append(elapsed).append("ms)\n");
        } catch (Exception e) {
            sb.append("  API Endpoint: unreachable - ").append(e.getMessage()).append("\n");
        }

        sb.append("\n");
    }

    private String runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(Path.of(System.getProperty("user.dir")).toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.destroyForcibly();
        }
        return output.trim();
    }
}
