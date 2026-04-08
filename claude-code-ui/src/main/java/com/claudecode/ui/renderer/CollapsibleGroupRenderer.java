package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Renders collapsible message groups.
 * Task 59.4: Collapsible group renderer
 *
 * Features:
 * - Collapsed/expanded state
 * - Group summary
 * - Toggle interaction
 */
public class CollapsibleGroupRenderer {

    public enum State { COLLAPSED, EXPANDED }

    private final int defaultCollapsedLines;
    private final Consumer<String> onToggle;

    public CollapsibleGroupRenderer() {
        this(3, null);
    }

    public CollapsibleGroupRenderer(int defaultCollapsedLines) {
        this(defaultCollapsedLines, null);
    }

    public CollapsibleGroupRenderer(int defaultCollapsedLines, Consumer<String> onToggle) {
        this.defaultCollapsedLines = defaultCollapsedLines;
        this.onToggle = onToggle;
    }

    public String renderCollapsed(String groupId, String summary, int itemCount) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("┌─ ", AnsiColor.GRAY));
        sb.append(Ansi.styled("[collapsed]", AnsiStyle.DIM));
        sb.append(" ");
        sb.append(Ansi.colored(summary, AnsiColor.CYAN));
        sb.append(" (").append(Ansi.styled(itemCount + " items", AnsiColor.GRAY)).append(")");
        sb.append(" ");
        sb.append(Ansi.styled("[press Enter to expand]", AnsiStyle.DIM));
        sb.append(Ansi.colored(" ┐", AnsiColor.GRAY));
        return sb.toString();
    }

    public String renderExpanded(String groupId, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("┌─ ", AnsiColor.GRAY));
        sb.append(Ansi.styled("[expanded]", AnsiColor.CYAN));
        sb.append(Ansi.colored(" ┐", AnsiColor.GRAY));
        sb.append("\n");

        for (String line : lines) {
            sb.append(Ansi.colored("│ ", AnsiColor.GRAY));
            sb.append(line);
            sb.append("\n");
        }

        sb.append(Ansi.colored("└", AnsiColor.GRAY));
        sb.append("─".repeat(Math.max(0, 50)));
        sb.append(Ansi.colored("┘", AnsiColor.GRAY));

        return sb.toString();
    }

    public String renderToggleHint(State currentState) {
        return switch (currentState) {
            case COLLAPSED -> Ansi.styled("[Enter: expand]", AnsiStyle.DIM);
            case EXPANDED -> Ansi.styled("[Enter: collapse]", AnsiStyle.DIM);
        };
    }

    public void handleToggle(String groupId) {
        if (onToggle != null) {
            onToggle.accept(groupId);
        }
    }

    public String renderGroupSummary(String groupType, int count, long totalTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored(groupType, AnsiColor.YELLOW));
        sb.append(": ");
        sb.append(Ansi.styled(count + " calls", AnsiColor.GRAY));
        if (totalTokens > 0) {
            sb.append(" (").append(Ansi.styled(totalTokens + " tokens", AnsiColor.GRAY)).append(")");
        }
        return sb.toString();
    }
}