package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;
import com.claudecode.ui.MarkdownRenderer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders messages in bubble chat style.
 */
public class MessageBubbleRenderer {

    private final MarkdownRenderer markdown;
    private final int width;
    private final boolean showTime;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public MessageBubbleRenderer() {
        this(80, true);
    }

    public MessageBubbleRenderer(int width, boolean showTime) {
        this.width = width;
        this.showTime = showTime;
        this.markdown = new MarkdownRenderer();
    }

    public String renderUserMessage(String content, Instant time) {
        StringBuilder sb = new StringBuilder();
        if (showTime && time != null) {
            sb.append("  ").append(formatTime(time)).append(" ");
        }
        sb.append(Ansi.colored("╭─ ", AnsiColor.GREEN));
        sb.append(" ");
        String[] lines = wrap(content, width - 8);
        for (String line : lines) {
            sb.append("\n").append(Ansi.colored("│ ", AnsiColor.GREEN)).append(line);
        }
        sb.append("\n").append(Ansi.colored("╰──", AnsiColor.GREEN));
        sb.append(Ansi.styled("─".repeat(Math.max(0, width - 6)), AnsiStyle.DIM));
        return sb.toString();
    }

    public String renderAssistantMessage(String content, String model, Instant time) {
        StringBuilder sb = new StringBuilder();
        if (showTime && time != null) {
            sb.append("  ").append(formatTime(time));
            if (model != null) {
                sb.append(Ansi.styled(" • " + model, AnsiStyle.DIM));
            }
            sb.append(" ");
        }
        sb.append(Ansi.colored("╭─ ", AnsiColor.BLUE));
        sb.append(" ");
        String rendered = markdown.render(content);
        for (String line : rendered.split("\n", -1)) {
            sb.append("\n").append(Ansi.colored("│ ", AnsiColor.BLUE)).append(line);
        }
        sb.append("\n").append(Ansi.colored("╰──", AnsiColor.BLUE));
        sb.append(Ansi.styled("─".repeat(Math.max(0, width - 6)), AnsiStyle.DIM));
        return sb.toString();
    }

    public String renderToolUse(String name, String input) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  ").append(Ansi.colored("🔧", AnsiColor.CYAN));
        sb.append(" ").append(Ansi.colored(name, AnsiColor.YELLOW));
        if (input != null && !input.isEmpty()) {
            String preview = input.length() > 80 ? input.substring(0, 80) + "..." : input;
            sb.append(" ").append(Ansi.styled(preview, AnsiStyle.DIM));
        }
        return sb.toString();
    }

    public String renderToolResult(String result, boolean isError) {
        AnsiColor color = isError ? AnsiColor.RED : AnsiColor.GRAY;
        return Ansi.colored("  └─ " + result, color);
    }

    public String renderError(String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  ").append(Ansi.colored("╔══ Error ═════", AnsiColor.RED));
        for (String line : wrap(error, width - 10)) {
            sb.append("\n  ").append(Ansi.colored("║ ", AnsiColor.RED)).append(line);
        }
        sb.append("\n  ").append(Ansi.colored("╚" + "═".repeat(Math.max(0, width - 6)), AnsiColor.RED));
        return sb.toString();
    }

    public String renderThinking(String content, long seconds) {
        return Ansi.colored("  💭 ", AnsiColor.MAGENTA) +
               Ansi.styled("Thinking for " + seconds + "s", AnsiStyle.DIM);
    }

    private String formatTime(Instant time) {
        if (time == null) return "";
        return timeFormatter.format(time.atZone(ZoneId.systemDefault()));
    }

    private String[] wrap(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return new String[]{""};
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(" ");
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines.toArray(new String[0]);
    }
}
