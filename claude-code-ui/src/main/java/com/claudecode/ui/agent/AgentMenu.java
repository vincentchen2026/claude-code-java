package com.claudecode.ui.agent;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Agent list menu for viewing and managing agents.
 * Task 61.1: Agent list/Detail/Editor
 */
public class AgentMenu {

    private final PrintWriter writer;
    private final Consumer<String> onSelect;
    private final Consumer<String> onEdit;
    private final Consumer<String> onDelete;

    public AgentMenu(PrintWriter writer) {
        this.writer = writer;
        this.onSelect = null;
        this.onEdit = null;
        this.onDelete = null;
    }

    public AgentMenu(PrintWriter writer, Consumer<String> onSelect,
                     Consumer<String> onEdit, Consumer<String> onDelete) {
        this.writer = writer;
        this.onSelect = onSelect;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    public void renderAgentList(List<AgentInfo> agents) {
        writer.println();
        writer.println(Ansi.styled("┌─ Agents ────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + agents.size() + " agent(s)");
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        for (int i = 0; i < agents.size(); i++) {
            AgentInfo agent = agents.get(i);
            String prefix = "│ " + Ansi.colored("[" + (i + 1) + "]", AnsiColor.YELLOW) + " ";
            writer.println(prefix + Ansi.colored(agent.name(), AnsiColor.WHITE));

            if (agent.model() != null && !agent.model().isEmpty()) {
                writer.println(Ansi.styled("│     Model: ", AnsiColor.GRAY) + agent.model());
            }
            if (agent.status() != null && !agent.status().isEmpty()) {
                AnsiColor statusColor = getStatusColor(agent.status());
                writer.println(Ansi.styled("│     Status: ", AnsiColor.GRAY) + Ansi.colored(agent.status(), statusColor));
            }
            if (agent.toolCount() > 0) {
                writer.println(Ansi.styled("│     Tools: ", AnsiColor.GRAY) + agent.toolCount() + " allowed");
            }
            writer.println(Ansi.styled("│", AnsiColor.CYAN));
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [n]ew agent  [s]elect  [e]dit  [d]elete  [q]uit");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderAgentDetail(AgentInfo agent) {
        writer.println();
        writer.println(Ansi.styled("┌─ Agent Detail ──────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Name: " + Ansi.colored(agent.name(), AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Model: " + Ansi.colored(agent.model(), AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Status: " + Ansi.colored(agent.status(), getStatusColor(agent.status())));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Tools: " + agent.toolCount());

        if (agent.color() != null && !agent.color().isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Color: " + agent.color());
        }
        if (agent.description() != null && !agent.description().isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Desc: " + Ansi.styled(agent.description(), AnsiStyle.DIM));
        }
        writer.println(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
    }

    private AnsiColor getStatusColor(String status) {
        return switch (status.toLowerCase()) {
            case "running", "active" -> AnsiColor.GREEN;
            case "paused", "idle" -> AnsiColor.YELLOW;
            case "stopped", "error" -> AnsiColor.RED;
            default -> AnsiColor.GRAY;
        };
    }

    public record AgentInfo(
        String name,
        String model,
        String status,
        int toolCount,
        String color,
        String description
    ) {}
}