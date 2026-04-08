package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.time.Instant;
import java.time.Duration;

/**
 * Renders teammate messages with ANSI styling.
 * Task 59.x: Teammate message renderer
 *
 * Features:
 * - Teammate name badge
 * - Timestamp
 * - Indentation for nested messages
 */
public class TeammateMessageRenderer {

    private static final AnsiColor[] TEAMMATE_COLORS = {
        AnsiColor.CYAN,
        AnsiColor.MAGENTA,
        AnsiColor.GREEN,
        AnsiColor.YELLOW
    };

    public TeammateMessageRenderer() {
    }

    public String render(String teammateName, String message, Instant timestamp) {
        StringBuilder sb = new StringBuilder();

        AnsiColor color = getTeammateColor(teammateName);
        sb.append(Ansi.colored("👤 ", color));
        sb.append(Ansi.styled(teammateName, AnsiColor.WHITE, AnsiStyle.BOLD));
        if (timestamp != null) {
            sb.append(" ").append(formatTimestamp(timestamp));
        }
        sb.append("\n");

        for (String line : message.split("\n", -1)) {
            sb.append("  ").append(line).append("\n");
        }

        return sb.toString().trim();
    }

    public String renderCompact(String teammateName, String message) {
        AnsiColor color = getTeammateColor(teammateName);
        return Ansi.colored("👤 ", color) +
               Ansi.colored(teammateName, color) +
               ": " + Ansi.styled(message, AnsiStyle.DIM);
    }

    public String renderWithBadge(String teammateName, String badge, String message) {
        AnsiColor color = getTeammateColor(teammateName);
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("👤 ", color));
        sb.append(Ansi.colored(teammateName, color));
        if (badge != null && !badge.isEmpty()) {
            sb.append(" ").append(Ansi.colored("[" + badge + "]", AnsiColor.YELLOW));
        }
        sb.append("\n");
        sb.append("  ").append(message);
        return sb.toString();
    }

    private AnsiColor getTeammateColor(String teammateName) {
        if (teammateName == null || teammateName.isEmpty()) {
            return AnsiColor.CYAN;
        }
        int hash = teammateName.hashCode();
        return TEAMMATE_COLORS[Math.abs(hash) % TEAMMATE_COLORS.length];
    }

    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return "";
        }
        Duration age = Duration.between(timestamp, Instant.now());
        if (age.toMinutes() < 1) {
            return Ansi.styled("just now", AnsiStyle.DIM);
        } else if (age.toMinutes() < 60) {
            return Ansi.styled(age.toMinutes() + "m ago", AnsiStyle.DIM);
        } else if (age.toHours() < 24) {
            return Ansi.styled(age.toHours() + "h ago", AnsiStyle.DIM);
        } else {
            return Ansi.styled(age.toDays() + "d ago", AnsiStyle.DIM);
        }
    }
}