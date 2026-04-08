package com.claudecode.ui.mcp;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;

public class McpToolViewer {

    private final PrintWriter writer;
    private int selectedIndex;
    private final List<McpToolInfo> tools;

    public McpToolViewer(PrintWriter writer, List<McpToolInfo> tools) {
        this.writer = writer;
        this.tools = tools;
        this.selectedIndex = 0;
    }

    public void renderList() {
        writer.println();
        writer.println(Ansi.styled("┌─ MCP Tools ──────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + tools.size() + " tool(s) available");
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        for (int i = 0; i < tools.size(); i++) {
            McpToolInfo tool = tools.get(i);
            boolean isSelected = (i == selectedIndex);

            if (isSelected) {
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + Ansi.styled(" > ", AnsiColor.GREEN) + 
                    Ansi.styled(tool.name(), AnsiColor.WHITE, AnsiStyle.BOLD));
            } else {
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " + Ansi.colored(tool.name(), AnsiColor.GRAY));
            }
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [e]nter  [d]own  [u]p  [q]uit");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderDetail(McpToolInfo tool) {
        writer.println();
        writer.println(Ansi.styled("┌─ Tool Detail ───────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Name: " + Ansi.colored(tool.name(), AnsiColor.WHITE));
        
        if (tool.description() != null) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Desc: " + Ansi.styled(tool.description(), AnsiStyle.DIM));
        }
        
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Input Schema:");
        
        if (tool.inputSchema() != null) {
            String schema = tool.inputSchema().toString();
            String[] lines = wrapText(schema, 60);
            for (String line : lines) {
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + Ansi.styled("   " + line, AnsiColor.GRAY));
            }
        }
        
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [b]ack  [c]all  [q]uit");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void selectNext() {
        if (selectedIndex < tools.size() - 1) {
            selectedIndex++;
        }
    }

    public void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        }
    }

    public McpToolInfo getSelectedTool() {
        if (selectedIndex >= 0 && selectedIndex < tools.size()) {
            return tools.get(selectedIndex);
        }
        return null;
    }

    public void select(int index) {
        if (index >= 0 && index < tools.size()) {
            this.selectedIndex = index;
        }
    }

    private String[] wrapText(String text, int maxWidth) {
        if (text == null) return new String[0];
        if (text.length() <= maxWidth) {
            return new String[]{text};
        }

        int count = (text.length() + maxWidth - 1) / maxWidth;
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            int start = i * maxWidth;
            int end = Math.min(start + maxWidth, text.length());
            lines[i] = text.substring(start, end);
        }
        return lines;
    }

    public record McpToolInfo(
        String name,
        String description,
        Object inputSchema
    ) {}
}