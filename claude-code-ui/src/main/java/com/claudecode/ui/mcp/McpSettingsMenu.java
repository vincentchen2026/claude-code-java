package com.claudecode.ui.mcp;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * MCP settings panel for server management.
 * Task 62.1: MCP settings panel
 */
public class McpSettingsMenu {

    private final PrintWriter writer;
    private final Consumer<String> onSelect;
    private final Consumer<String> onToggle;
    private final Consumer<String> onConfigure;

    public McpSettingsMenu(PrintWriter writer) {
        this.writer = writer;
        this.onSelect = null;
        this.onToggle = null;
        this.onConfigure = null;
    }

    public McpSettingsMenu(PrintWriter writer, Consumer<String> onSelect,
                           Consumer<String> onToggle, Consumer<String> onConfigure) {
        this.writer = writer;
        this.onSelect = onSelect;
        this.onToggle = onToggle;
        this.onConfigure = onConfigure;
    }

    public void renderMcpServers(List<McpServerInfo> servers) {
        writer.println();
        writer.println(Ansi.styled("┌─ MCP Servers ─────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + servers.size() + " server(s) configured");
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        for (int i = 0; i < servers.size(); i++) {
            McpServerInfo server = servers.get(i);
            String prefix = "│ " + Ansi.colored("[" + (i + 1) + "]", AnsiColor.YELLOW) + " ";
            String enabledStr = server.enabled() ? Ansi.colored("●", AnsiColor.GREEN) : Ansi.colored("○", AnsiColor.GRAY);
            writer.println(prefix + enabledStr + " " + Ansi.colored(server.name(), AnsiColor.WHITE));

            String transportIcon = getTransportIcon(server.transport());
            writer.println(Ansi.styled("│     Transport: ", AnsiColor.GRAY) + transportIcon + " " + server.transport());
            writer.println(Ansi.styled("│     Tools: ", AnsiColor.GRAY) + server.toolCount() + " available");
            if (server.status() != null && !server.status().isEmpty()) {
                writer.println(Ansi.styled("│     Status: ", AnsiColor.GRAY) + server.status());
            }
            writer.println(Ansi.styled("│", AnsiColor.CYAN));
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]dd server  [t]oggle  [c]onfigure  [r]emove  [q]uit");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String getTransportIcon(String transport) {
        return switch (transport.toLowerCase()) {
            case "stdio" -> "🔀";
            case "sse", "server-sent-events" -> "📡";
            case "websocket", "ws" -> "🔌";
            default -> "⚙️";
        };
    }

    public record McpServerInfo(
        String name,
        String transport,
        int toolCount,
        boolean enabled,
        String status
    ) {}
}