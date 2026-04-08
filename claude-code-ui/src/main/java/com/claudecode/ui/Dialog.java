package com.claudecode.ui;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic dialog framework for terminal-based dialogs.
 * Task 64.5: Used for BridgeDialog, GlobalSearchDialog, QuickOpenDialog, ExportDialog, Onboarding, etc.
 */
public class Dialog {

    private final PrintWriter writer;
    private final int width;
    private final String title;
    private final List<String> contentLines = new ArrayList<>();
    private final List<DialogAction> actions = new ArrayList<>();

    public record DialogAction(String key, String label, Runnable handler) {}

    public Dialog(PrintWriter writer, int width, String title) {
        this.writer = writer;
        this.width = width;
        this.title = title;
    }

    public Dialog addContent(String line) {
        contentLines.add(line);
        return this;
    }

    public Dialog addContent(List<String> lines) {
        contentLines.addAll(lines);
        return this;
    }

    public Dialog addAction(String key, String label, Runnable handler) {
        actions.add(new DialogAction(key, label, handler));
        return this;
    }

    /**
     * Render the dialog to the terminal.
     */
    public void render() {
        writer.println();
        writer.println(Ansi.styled("┌─ " + title + " " + "─".repeat(Math.max(0, width - title.length() - 4)), AnsiColor.CYAN));

        for (String line : contentLines) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + line);
        }

        if (!actions.isEmpty()) {
            writer.println(Ansi.styled("├" + "─".repeat(width - 1), AnsiColor.CYAN));
            StringBuilder actionLine = new StringBuilder(Ansi.styled("│", AnsiColor.CYAN) + " ");
            for (int i = 0; i < actions.size(); i++) {
                DialogAction action = actions.get(i);
                actionLine.append("[").append(Ansi.colored(action.key(), AnsiColor.YELLOW)).append("]")
                    .append(action.label());
                if (i < actions.size() - 1) {
                    actionLine.append("  ");
                }
            }
            writer.println(actionLine);
        }

        writer.println(Ansi.styled("└" + "─".repeat(width - 1), AnsiColor.CYAN));
        writer.println();
        writer.flush();
    }

    /**
     * Render a simple confirmation dialog.
     */
    public static void confirm(PrintWriter writer, int width, String message, Runnable onYes, Runnable onNo) {
        Dialog dialog = new Dialog(writer, width, "Confirm");
        dialog.addContent(message);
        dialog.addAction("y", "es", onYes);
        dialog.addAction("n", "o", onNo);
        dialog.render();
    }

    /**
     * Render an info dialog.
     */
    public static void info(PrintWriter writer, int width, String title, String message) {
        Dialog dialog = new Dialog(writer, width, title);
        dialog.addContent(message);
        dialog.addAction("enter", "to close", () -> {});
        dialog.render();
    }

    /**
     * Render an error dialog.
     */
    public static void error(PrintWriter writer, int width, String title, String message) {
        Dialog dialog = new Dialog(writer, width, "Error: " + title);
        dialog.addContent(Ansi.colored(message, AnsiColor.RED));
        dialog.addAction("enter", "to close", () -> {});
        dialog.render();
    }
}
