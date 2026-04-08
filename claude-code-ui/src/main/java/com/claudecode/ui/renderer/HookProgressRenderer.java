package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

/**
 * Renders hook progress messages with ANSI styling.
 * Task 59.x: Hook progress renderer
 *
 * Features:
 * - Hook type indicator
 * - Progress percentage
 * - Status message
 */
public class HookProgressRenderer {

    public HookProgressRenderer() {
    }

    public String render(String hookName, String status) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("🪝 ", AnsiColor.MAGENTA));
        sb.append(Ansi.colored(hookName, AnsiColor.CYAN));
        if (status != null && !status.isEmpty()) {
            sb.append(": ").append(Ansi.styled(status, AnsiStyle.DIM));
        }
        return sb.toString();
    }

    public String renderWithProgress(String hookName, String status, int progressPercent) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("🪝 ", AnsiColor.MAGENTA));
        sb.append(Ansi.colored(hookName, AnsiColor.CYAN));
        if (status != null && !status.isEmpty()) {
            sb.append(": ").append(Ansi.styled(status, AnsiStyle.DIM));
        }
        sb.append(" ").append(formatProgressBar(progressPercent));
        return sb.toString();
    }

    public String renderComplete(String hookName, String result) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("✓ ", AnsiColor.GREEN));
        sb.append(Ansi.colored(hookName, AnsiColor.CYAN));
        if (result != null && !result.isEmpty()) {
            sb.append(" completed");
        }
        return sb.toString();
    }

    public String renderError(String hookName, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("✗ ", AnsiColor.RED));
        sb.append(Ansi.colored(hookName, AnsiColor.CYAN));
        if (error != null && !error.isEmpty()) {
            sb.append(": ").append(Ansi.colored(error, AnsiColor.RED));
        }
        return sb.toString();
    }

    private String formatProgressBar(int percent) {
        int barWidth = 20;
        int filled = (int) ((percent / 100.0) * barWidth);
        StringBuilder sb = new StringBuilder("[");
        sb.append(Ansi.colored(String.valueOf(filled).repeat(Math.max(0, filled)), AnsiColor.GREEN));
        sb.append(Ansi.styled(String.valueOf(barWidth - filled).repeat(Math.max(0, barWidth - filled)), AnsiColor.GRAY));
        sb.append("]");
        sb.append(" ").append(Ansi.colored(percent + "%", AnsiColor.CYAN));
        return sb.toString();
    }
}