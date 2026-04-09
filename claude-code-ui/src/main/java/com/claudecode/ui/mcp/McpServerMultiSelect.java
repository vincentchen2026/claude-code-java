package com.claudecode.ui.mcp;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class McpServerMultiSelect {

    private final PrintWriter writer;
    private final List<ServerOption> servers;
    private final Set<Integer> selectedIndices;
    private int cursorIndex;
    private TransportFilter transportFilter;
    private boolean confirmMode;

    public McpServerMultiSelect(PrintWriter writer, List<ServerOption> servers) {
        this.writer = writer;
        this.servers = new ArrayList<>(servers);
        this.selectedIndices = new HashSet<>();
        this.cursorIndex = 0;
        this.transportFilter = TransportFilter.ALL;
        this.confirmMode = false;
    }

    public void render() {
        writer.println();
        writer.println(Ansi.styled("┌─ MCP Server Selection ────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.styled("Filter:", AnsiColor.GRAY) + " " + renderFilterLabel());
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        int displayedIndex = 0;
        for (int i = 0; i < servers.size(); i++) {
            ServerOption server = servers.get(i);
            if (!matchesFilter(server)) continue;

            boolean isSelected = selectedIndices.contains(i);
            boolean isCursor = (displayedIndex == cursorIndex);

            String checkbox = isSelected
                ? Ansi.colored("[x]", AnsiColor.GREEN)
                : Ansi.colored("[ ]", AnsiColor.GRAY);

            String cursor = isCursor ? Ansi.styled(" > ", AnsiColor.YELLOW) : "   ";

            writer.println(Ansi.styled("│", AnsiColor.CYAN) + cursor + checkbox + " " +
                Ansi.colored(server.name(), isSelected ? AnsiColor.WHITE : AnsiColor.GRAY) +
                " " + Ansi.styled("(" + server.transport() + ")", AnsiStyle.DIM));

            displayedIndex++;
        }

        if (displayedIndex == 0) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.colored("(no servers match filter)", AnsiColor.GRAY));
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        if (confirmMode) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + Ansi.colored(" Confirm action on " + selectedIndices.size() + " server(s)?", AnsiColor.YELLOW));
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [y]es  [n]o");
        } else {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [space] toggle  [a]ll stdio  [r]emote  [e]verything");
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [d]elete  [c]onfigure  [q]uit");
        }
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String renderFilterLabel() {
        return switch (transportFilter) {
            case ALL -> Ansi.colored("All", AnsiColor.WHITE);
            case STDIO -> Ansi.colored("stdio", AnsiColor.CYAN);
            case REMOTE -> Ansi.colored("remote", AnsiColor.MAGENTA);
            case AGENT -> Ansi.colored("agent", AnsiColor.YELLOW);
        };
    }

    private boolean matchesFilter(ServerOption server) {
        return switch (transportFilter) {
            case ALL -> true;
            case STDIO -> server.transport().equalsIgnoreCase("stdio");
            case REMOTE -> server.transport().equalsIgnoreCase("remote") ||
                          server.transport().equalsIgnoreCase("sse") ||
                          server.transport().equalsIgnoreCase("websocket");
            case AGENT -> server.transport().equalsIgnoreCase("agent");
        };
    }

    public void toggleCurrent() {
        int displayedIndex = 0;
        for (int i = 0; i < servers.size(); i++) {
            if (!matchesFilter(servers.get(i))) continue;
            if (displayedIndex == cursorIndex) {
                if (selectedIndices.contains(i)) {
                    selectedIndices.remove(i);
                } else {
                    selectedIndices.add(i);
                }
                return;
            }
            displayedIndex++;
        }
    }

    public void selectAll() {
        selectedIndices.clear();
        for (int i = 0; i < servers.size(); i++) {
            if (matchesFilter(servers.get(i))) {
                selectedIndices.add(i);
            }
        }
    }

    public void moveCursorDown() {
        int visibleCount = (int) servers.stream().filter(this::matchesFilter).count();
        if (cursorIndex < visibleCount - 1) {
            cursorIndex++;
        }
    }

    public void moveCursorUp() {
        if (cursorIndex > 0) {
            cursorIndex--;
        }
    }

    public void setTransportFilter(TransportFilter filter) {
        this.transportFilter = filter;
        this.cursorIndex = 0;
    }

    public void enterConfirmMode() {
        if (!selectedIndices.isEmpty()) {
            confirmMode = true;
        }
    }

    public void exitConfirmMode() {
        confirmMode = false;
    }

    public boolean isConfirmMode() {
        return confirmMode;
    }

    public List<ServerOption> getSelectedServers() {
        List<ServerOption> selected = new ArrayList<>();
        for (int idx : selectedIndices) {
            selected.add(servers.get(idx));
        }
        return selected;
    }

    public void executeAction(BiConsumer<ServerOption, PrintWriter> action, PrintWriter writer) {
        for (int idx : selectedIndices) {
            action.accept(servers.get(idx), writer);
        }
    }

    public enum TransportFilter {
        ALL, STDIO, REMOTE, AGENT
    }

    public record ServerOption(
        String name,
        String transport,
        String command,
        String url,
        boolean enabled
    ) {}
}