package com.claudecode.ui.mcp;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;

/**
 * MCP server list panel showing server status.
 * Task 62.2: MCP server list panel
 */
public class McpServerList {

    private final PrintWriter writer;

    public McpServerList(PrintWriter writer) {
        this.writer = writer;
    }

    public void render(List<McpServerInfo> servers) {
        writer.println();
        writer.println(Ansi.styled("┌─ MCP Server Status ──────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + Ansi.styled(" Server              Transport  Status    Tools", AnsiStyle.DIM));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        for (McpServerInfo server : servers) {
            AnsiColor statusColor = getStatusColor(server.status());
            String statusStr = server.status() != null ? server.status() : "unknown";
            writer.println(Ansi.styled("│ ", AnsiColor.CYAN) +
                padRight(server.name(), 18) +
                padRight(server.transport(), 11) +
                Ansi.colored(padRight(statusStr, 10), statusColor) +
                server.toolCount());
        }

        writer.println(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.flush();
    }

    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    private String padRight(String s, int width, AnsiColor color) {
        return Ansi.colored(padRight(s, width), color);
    }

    private AnsiColor getStatusColor(String status) {
        if (status == null) return AnsiColor.GRAY;
        return switch (status.toLowerCase()) {
            case "running", "connected" -> AnsiColor.GREEN;
            case "connecting", "starting" -> AnsiColor.YELLOW;
            case "disconnected", "error" -> AnsiColor.RED;
            default -> AnsiColor.GRAY;
        };
    }

    public record McpServerInfo(
        String name,
        String transport,
        String status,
        int toolCount
    ) {}
}