package com.claudecode.ui;

import java.io.PrintWriter;

/**
 * Status line renderer — single line display at bottom of terminal.
 * Task 64.4: Shows model, tokens, cost, cwd, permissions mode.
 */
public class StatusLine {

    private final PrintWriter writer;
    private final int terminalWidth;

    private String model = "unknown";
    private long inputTokens = 0;
    private long outputTokens = 0;
    private double cost = 0.0;
    private String cwd = "";
    private String permissionMode = "ask";
    private String agentName = "main";

    public StatusLine(PrintWriter writer, int terminalWidth) {
        this.writer = writer;
        this.terminalWidth = terminalWidth;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setTokens(long inputTokens, long outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setCwd(String cwd) {
        this.cwd = cwd;
    }

    public void setPermissionMode(String mode) {
        this.permissionMode = mode;
    }

    public void setAgentName(String name) {
        this.agentName = name;
    }

    /**
     * Render the status line to the terminal.
     */
    public void render() {
        StringBuilder line = new StringBuilder();

        // Save cursor position
        line.append("\u001B[s");

        // Move to bottom line
        line.append("\u001B[999;1H");

        // Clear line
        line.append("\u001B[K");

        // Background
        line.append("\u001B[48;5;235m");

        // Agent name
        line.append(Ansi.colored(" " + agentName + " ", AnsiColor.CYAN));
        line.append(" ");

        // Model
        line.append(Ansi.styled(model, AnsiStyle.DIM));
        line.append(" ");

        // Tokens
        line.append(Ansi.colored("↑" + formatTokens(inputTokens), AnsiColor.GREEN));
        line.append(Ansi.colored(" ↓" + formatTokens(outputTokens), AnsiColor.MAGENTA));
        line.append(" ");

        // Cost
        if (cost > 0) {
            line.append(Ansi.colored("$" + String.format("%.2f", cost), AnsiColor.YELLOW));
            line.append(" ");
        }

        // Permission mode indicator
        String permIndicator = switch (permissionMode.toLowerCase()) {
            case "auto" -> Ansi.colored("⚡auto", AnsiColor.GREEN);
            case "yolo" -> Ansi.colored("⚡yolo", AnsiColor.RED);
            default -> Ansi.styled("ask", AnsiStyle.DIM);
        };
        line.append(permIndicator);
        line.append(" ");

        // CWD (truncated)
        String shortCwd = shortenCwd(cwd);
        line.append(Ansi.styled(shortCwd, AnsiStyle.DIM));

        // Reset
        line.append("\u001B[0m");

        // Restore cursor
        line.append("\u001B[u");

        writer.print(line);
        writer.flush();
    }

    /**
     * Clear the status line.
     */
    public void clear() {
        writer.print("\u001B[s");
        writer.print("\u001B[999;1H");
        writer.print("\u001B[K");
        writer.print("\u001B[u");
        writer.flush();
    }

    private String formatTokens(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format("%.1fm", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format("%.1fk", tokens / 1_000.0);
        }
        return String.valueOf(tokens);
    }

    private String shortenCwd(String path) {
        if (path == null || path.isEmpty()) return "";
        // Show last 3 path components
        String[] parts = path.replace('\\', '/').split("/");
        if (parts.length <= 3) return path;
        return "~/" + String.join("/", java.util.Arrays.copyOfRange(parts, parts.length - 3, parts.length));
    }
}
