package com.claudecode.ui.task;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ShellTaskDetail {

    private final PrintWriter writer;
    private final String taskId;
    private final Path outputFile;
    private volatile boolean streaming;
    private Future<?> streamingTask;

    public ShellTaskDetail(PrintWriter writer, String taskId, Path outputFile) {
        this.writer = writer;
        this.taskId = taskId;
        this.outputFile = outputFile;
        this.streaming = false;
    }

    public void renderHeader() {
        writer.println();
        writer.println(Ansi.styled("┌─ Shell Task ───────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Task: " + Ansi.colored(taskId, AnsiColor.WHITE));
        if (outputFile != null) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Output: " + Ansi.colored(outputFile.toString(), AnsiColor.GRAY));
        }
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
    }

    public void renderStreaming(ExecutorService executor) {
        if (streaming) return;

        streaming = true;
        streamingTask = executor.submit(() -> {
            try {
                if (outputFile != null && java.nio.file.Files.exists(outputFile)) {
                    try (BufferedReader reader = java.nio.file.Files.newBufferedReader(outputFile)) {
                        String line;
                        while (streaming && (line = reader.readLine()) != null) {
                            writer.println(Ansi.colored(line, AnsiColor.GRAY));
                            writer.flush();
                        }
                    }
                }
            } catch (Exception e) {
                writer.println(Ansi.colored("Error reading output: " + e.getMessage(), AnsiColor.RED));
            }
        });
    }

    public void renderFromStream(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(Ansi.colored(line, AnsiColor.GRAY));
                writer.flush();
            }
        } catch (Exception e) {
            writer.println(Ansi.colored("Error: " + e.getMessage(), AnsiColor.RED));
        }
    }

    public void renderTail(int lines) {
        if (outputFile == null || !java.nio.file.Files.exists(outputFile)) {
            writer.println(Ansi.colored("(no output available)", AnsiColor.GRAY));
            return;
        }

        try {
            String content = java.nio.file.Files.readString(outputFile);
            String[] allLines = content.split("\n");
            int start = Math.max(0, allLines.length - lines);
            for (int i = start; i < allLines.length; i++) {
                writer.println(Ansi.colored(allLines[i], AnsiColor.GRAY));
            }
        } catch (Exception e) {
            writer.println(Ansi.colored("Error reading tail: " + e.getMessage(), AnsiColor.RED));
        }
    }

    public void stopStreaming() {
        streaming = false;
        if (streamingTask != null) {
            streamingTask.cancel(false);
        }
    }

    public void renderProgress(TaskProgress progress) {
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + 
            Ansi.colored(progress.status(), getStatusColor(progress.status())) +
            " " + Ansi.colored(progress.message(), AnsiColor.GRAY));
    }

    private AnsiColor getStatusColor(String status) {
        if (status == null) return AnsiColor.GRAY;
        return switch (status.toLowerCase()) {
            case "running", "active" -> AnsiColor.GREEN;
            case "pending", "queued" -> AnsiColor.YELLOW;
            case "completed", "done" -> AnsiColor.CYAN;
            case "failed", "error" -> AnsiColor.RED;
            default -> AnsiColor.GRAY;
        };
    }

    public void renderFooter() {
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [s]top  [r]efresh  [t]ail  [c]lose");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public record TaskProgress(
        String status,
        String message,
        int exitCode
    ) {}
}