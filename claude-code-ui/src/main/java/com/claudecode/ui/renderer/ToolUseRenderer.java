package com.claudecode.ui.renderer;

import com.claudecode.core.message.ToolUseBlock;
import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders tool use blocks with ANSI styling.
 * Task 59.7: Tool use progress indicator
 *
 * Features:
 * - Tool name with icon
 * - Input preview (truncated)
 * - Progress indicator for long-running tools
 * - Error state
 */
public class ToolUseRenderer {

    private static final int MAX_INPUT_PREVIEW = 100;
    private static final Map<String, String> TOOL_ICONS = Map.of(
        "Bash", "🔧",
        "Write", "📝",
        "Read", "📖",
        "Edit", "✏️",
        "Glob", "🔍",
        "Grep", "🔎",
        "WebSearch", "🌐",
        "WebFetch", "🌍",
        "Agent", "🤖",
        "Task", "📋"
    );

    public ToolUseRenderer() {
    }

    public String render(ToolUseBlock block) {
        if (block == null) {
            return "";
        }

        String toolIcon = TOOL_ICONS.getOrDefault(block.name(), "🔧");
        String inputPreview = formatInput(block.input());

        return Ansi.colored("  " + toolIcon + " ", AnsiColor.CYAN) +
               Ansi.colored(block.name(), AnsiColor.YELLOW) +
               " " + Ansi.styled(inputPreview, AnsiStyle.DIM);
    }

    public String renderCompact(ToolUseBlock block) {
        if (block == null) {
            return "";
        }

        String toolIcon = TOOL_ICONS.getOrDefault(block.name(), "🔧");
        return Ansi.colored("  " + toolIcon + " ", AnsiColor.CYAN) +
               Ansi.colored(block.name(), AnsiColor.YELLOW);
    }

    public String renderWithProgress(ToolUseBlock block, String progress) {
        if (block == null) {
            return "";
        }

        String toolIcon = TOOL_ICONS.getOrDefault(block.name(), "🔧");
        String inputPreview = formatInput(block.input());

        return Ansi.colored("  " + toolIcon + " ", AnsiColor.CYAN) +
               Ansi.colored(block.name(), AnsiColor.YELLOW) +
               " " + Ansi.styled(inputPreview, AnsiStyle.DIM) +
               " " + Ansi.colored(progress, AnsiColor.GREEN);
    }

    public String renderWithError(ToolUseBlock block, String error) {
        if (block == null) {
            return "";
        }

        String toolIcon = "⚠️";
        String inputPreview = formatInput(block.input());

        return Ansi.colored("  " + toolIcon + " ", AnsiColor.RED) +
               Ansi.colored(block.name(), AnsiColor.YELLOW) +
               " " + Ansi.styled(inputPreview, AnsiStyle.DIM) +
               " " + Ansi.colored("[ERROR: " + error + "]", AnsiColor.RED);
    }

    private String formatInput(Object input) {
        if (input == null) {
            return "";
        }
        String str = input.toString();
        if (str.length() > MAX_INPUT_PREVIEW) {
            return str.substring(0, MAX_INPUT_PREVIEW - 3) + "...";
        }
        return str;
    }
}