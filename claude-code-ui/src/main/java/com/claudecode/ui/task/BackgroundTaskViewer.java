package com.claudecode.ui.task;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;

/**
 * Background task viewer for real-time output display.
 * Task 63.2: Background task viewer
 */
public class BackgroundTaskViewer {

    private final PrintWriter writer;

    public BackgroundTaskViewer(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderTaskHeader(String taskId, String type, String status) {
        writer.println();
        writer.println(Ansi.styled("┌─ Task: " + taskId + " ────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Type: " + Ansi.colored(type, AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Status: " + Ansi.colored(status, getStatusColor(status)));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│ Output:", AnsiStyle.DIM));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
    }

    public void renderOutput(String output) {
        if (output != null && !output.isEmpty()) {
            for (String line : output.split("\n", -1)) {
                writer.println(Ansi.styled("│ ", AnsiColor.CYAN) + Ansi.styled(line, AnsiStyle.DIM));
            }
        }
    }

    public void renderOutputLine(String line) {
        writer.print(Ansi.styled("│ ", AnsiColor.CYAN));
        writer.println(Ansi.styled(line, AnsiStyle.DIM));
    }

    public void renderTaskFooter() {
        writer.println(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("  [c]ancel  [r]efresh  [q]uit", AnsiStyle.DIM));
        writer.flush();
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
}