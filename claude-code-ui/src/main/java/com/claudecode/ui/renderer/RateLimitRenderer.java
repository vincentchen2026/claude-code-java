package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Enhanced rate limit renderer with usage percentage and reset time.
 */
public class RateLimitRenderer {

    private final int terminalWidth;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RateLimitRenderer() {
        this(80);
    }

    public RateLimitRenderer(int terminalWidth) {
        this.terminalWidth = terminalWidth;
    }

    public String render(String limitName, long usedTokens, long totalTokens, Instant resetsAt) {
        StringBuilder sb = new StringBuilder();
        double pct = totalTokens > 0 ? (double) usedTokens / totalTokens : 0;

        sb.append("\n");
        sb.append(Ansi.colored("  ╔══ Rate Limit ═════════════════════════════════════════════╗", AnsiColor.YELLOW));
        sb.append("\n");
        sb.append(Ansi.colored("  ║ ", AnsiColor.YELLOW));
        sb.append(Ansi.styled("⚠ " + limitName, AnsiStyle.BOLD));
        sb.append("\n");
        sb.append(Ansi.colored("  ║ ", AnsiColor.YELLOW));

        int barWidth = Math.max(20, terminalWidth - 20);
        int filled = (int) (pct * barWidth);
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        bar.append(Ansi.colored("█".repeat(Math.max(0, filled)), pct > 0.9 ? AnsiColor.RED : pct > 0.7 ? AnsiColor.YELLOW : AnsiColor.GREEN));
        bar.append(Ansi.colored("░".repeat(Math.max(0, barWidth - filled)), AnsiColor.GRAY));
        bar.append("] ");
        bar.append(Ansi.styled(String.format("%.1f%%", pct * 100)));
        sb.append(bar);
        sb.append("\n");

        sb.append(Ansi.colored("  ║ ", AnsiColor.YELLOW));
        sb.append(formatToken(usedTokens));
        sb.append(" / ");
        sb.append(Ansi.colored(formatToken(totalTokens), AnsiColor.GRAY));
        sb.append(" tokens\n");

        if (resetsAt != null) {
            sb.append(Ansi.colored("  ║ ", AnsiColor.YELLOW));
            sb.append(Ansi.styled("Resets: " + formatReset(resetsAt), AnsiStyle.DIM));
            sb.append("\n");
        }

        sb.append(Ansi.colored("  ╚" + "═".repeat(Math.max(0, terminalWidth - 6)) + "╝", AnsiColor.YELLOW));
        sb.append("\n");
        return sb.toString();
    }

    public String renderCompact(long used, long total, Instant resetsAt) {
        double pct = total > 0 ? (double) used / total : 0;
        AnsiColor color = pct > 0.9 ? AnsiColor.RED : pct > 0.7 ? AnsiColor.YELLOW : AnsiColor.GREEN;
        return Ansi.colored("⚠ Rate limit: ", AnsiColor.YELLOW)
                + Ansi.colored(String.format("%.0f%%", pct * 100), color)
                + " (" + formatToken(used) + "/" + formatToken(total) + ")";
    }

    public String renderUsageLine(long used, long total) {
        double pct = total > 0 ? (double) used / total : 0;
        AnsiColor color = pct > 0.9 ? AnsiColor.RED : pct > 0.7 ? AnsiColor.YELLOW : AnsiColor.GREEN;
        int barWidth = 20;
        int filled = (int) (pct * barWidth);
        return Ansi.colored(formatToken(used), AnsiColor.YELLOW)
                + "/" + Ansi.colored(formatToken(total), AnsiColor.GRAY) + " "
                + Ansi.colored("[" + "█".repeat(filled) + "░".repeat(barWidth - filled) + "] ", color);
    }

    private String formatToken(long tokens) {
        if (tokens >= 1_000_000) return String.format("%.1fM", tokens / 1_000_000.0);
        if (tokens >= 1_000) return String.format("%.1fK", tokens / 1_000.0);
        return String.valueOf(tokens);
    }

    private String formatReset(Instant at) {
        if (at == null) return "unknown";
        long secs = ChronoUnit.SECONDS.between(Instant.now(), at);
        if (secs <= 0) return "now";
        if (secs < 60) return secs + "s";
        if (secs < 3600) return (secs / 60) + "m";
        return timeFormatter.format(at.atZone(ZoneId.systemDefault()));
    }
}
