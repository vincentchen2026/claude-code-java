package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced thinking block renderer with shimmer animation.
 */
public class ThinkingBlockRenderer {

    private static final int MAX_PREVIEW = 100;
    private static final Duration SHIMMER_PERIOD = Duration.ofMillis(1500);

    private final AtomicReference<Instant> thinkingStart = new AtomicReference<>();

    public ThinkingBlockRenderer() {}

    public void startThinking() {
        thinkingStart.set(Instant.now());
    }

    public void stopThinking() {
        thinkingStart.set(null);
    }

    public String render(String thinking) {
        if (thinking == null || thinking.isEmpty()) return "";
        Instant start = thinkingStart.get();
        Duration duration = start != null ? Duration.between(start, Instant.now()) : Duration.ZERO;

        String timeStr = formatDuration(duration);
        String tokenStr = estimateTokens(thinking);

        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("  +- ", AnsiColor.GRAY));
        sb.append(Ansi.colored("Thinking ", AnsiColor.MAGENTA));
        sb.append(Ansi.styled(timeStr + " " + tokenStr, AnsiStyle.DIM));
        sb.append(Ansi.colored("-+", AnsiColor.GRAY));
        sb.append("\n");

        String display = thinking.length() > MAX_PREVIEW ? thinking.substring(0, MAX_PREVIEW) + "..." : thinking;
        for (String line : display.split("\n", -1)) {
            sb.append(Ansi.styled("  | " + line, AnsiStyle.DIM));
            sb.append("\n");
        }

        if (thinking.length() > MAX_PREVIEW) {
            sb.append(Ansi.colored("  | ... " + (thinking.length() - MAX_PREVIEW) + " more", AnsiColor.YELLOW));
            sb.append("\n");
        }

        sb.append(Ansi.colored("  +" + repeat("-", 50) + "-+", AnsiColor.GRAY));
        return sb.toString();
    }

    public String renderCompact(String thinking) {
        if (thinking == null || thinking.isEmpty()) return "";
        Instant start = thinkingStart.get();
        Duration duration = start != null ? Duration.between(start, Instant.now()) : Duration.ZERO;
        return Ansi.colored("  Thinking ", AnsiColor.MAGENTA)
                + Ansi.styled(formatDuration(duration) + " " + estimateTokens(thinking), AnsiStyle.DIM);
    }

    public String renderWithShimmer(String thinking, double phase) {
        if (thinking == null || thinking.isEmpty()) return "";
        Instant start = thinkingStart.get();
        Duration duration = start != null ? Duration.between(start, Instant.now()) : Duration.ZERO;

        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("  +- ", AnsiColor.GRAY));
        sb.append(applyShimmer("Thinking ", phase));
        sb.append(Ansi.styled(formatDuration(duration), AnsiStyle.DIM));
        sb.append(Ansi.colored("-+", AnsiColor.GRAY));
        sb.append("\n");

        String display = thinking.length() > MAX_PREVIEW ? thinking.substring(0, MAX_PREVIEW) : thinking;
        for (String line : display.split("\n", -1)) {
            sb.append(Ansi.styled("  | " + line, AnsiStyle.DIM));
            sb.append("\n");
        }
        sb.append(Ansi.colored("  +" + repeat("-", 50) + "-+", AnsiColor.GRAY));
        return sb.toString();
    }

    private String applyShimmer(String text, double phase) {
        if (text == null || text.isEmpty()) return "";
        int hl = Math.max(1, (int)(phase * text.length()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            if (i < hl) {
                sb.append(Ansi.styled(ch, AnsiStyle.BOLD));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static double calcPhase(Instant start) {
        if (start == null) return 0;
        long ms = Duration.between(start, Instant.now()).toMillis() % SHIMMER_PERIOD.toMillis();
        return (double) ms / SHIMMER_PERIOD.toMillis();
    }

    private String formatDuration(Duration d) {
        long s = d.getSeconds();
        return s < 60 ? s + "s" : (s / 60) + "m " + (s % 60) + "s";
    }

    private String estimateTokens(String text) {
        int t = text.length() / 4;
        return t >= 1000 ? String.format("%.1fk tokens", t / 1000.0) : t + " tokens";
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
