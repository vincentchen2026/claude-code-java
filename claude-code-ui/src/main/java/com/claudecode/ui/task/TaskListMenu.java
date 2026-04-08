package com.claudecode.ui.task;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Task list menu with status color coding.
 * Task 63.1: Task list menu
 */
public class TaskListMenu {

    private final PrintWriter writer;

    public TaskListMenu(PrintWriter writer) {
        this.writer = writer;
    }

    public void render(List<TaskInfo> tasks) {
        writer.println();
        writer.println(Ansi.styled("┌─ Tasks ──────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + tasks.size() + " task(s)");
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        for (int i = 0; i < tasks.size(); i++) {
            TaskInfo task = tasks.get(i);
            AnsiColor statusColor = getStatusColor(task.status());

            String prefix = "│ " + Ansi.colored("[" + (i + 1) + "]", AnsiColor.YELLOW) + " ";
            writer.println(prefix + Ansi.colored(task.id(), AnsiColor.WHITE) +
                " " + Ansi.colored(task.status(), statusColor));

            if (task.description() != null && !task.description().isEmpty()) {
                writer.println(Ansi.styled("│     ", AnsiColor.CYAN) +
                    Ansi.styled(truncate(task.description(), 50), AnsiStyle.DIM));
            }

            if (task.agent() != null && !task.agent().isEmpty()) {
                writer.println(Ansi.styled("│     Agent: ", AnsiColor.GRAY) + task.agent());
            }
            writer.println(Ansi.styled("│", AnsiColor.CYAN));
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [v]iew  [s]top  [d]elete  [q]uit");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private AnsiColor getStatusColor(String status) {
        if (status == null) return AnsiColor.GRAY;
        return switch (status.toLowerCase()) {
            case "running", "active" -> AnsiColor.GREEN;
            case "pending", "queued" -> AnsiColor.YELLOW;
            case "completed", "done" -> AnsiColor.CYAN;
            case "failed", "error" -> AnsiColor.RED;
            case "stopped", "cancelled" -> AnsiColor.MAGENTA;
            default -> AnsiColor.GRAY;
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public record TaskInfo(
        String id,
        String description,
        String status,
        String agent
    ) {}
}