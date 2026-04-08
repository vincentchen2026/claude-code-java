package com.claudecode.ui;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Terminal notification system for displaying progress and alerts.
 * Supports OSC 9;4 progress bar and OSC 9;2 notifications.
 */
public class TerminalNotification {

    private final PrintWriter writer;
    private final TerminalProtocols.TerminalCapabilities capabilities;
    private final AtomicBoolean notificationActive = new AtomicBoolean(false);
    private volatile String currentTitle = "";
    private volatile int currentProgress = 0;
    private volatile Instant startTime;

    public TerminalNotification(PrintWriter writer) {
        this.writer = writer;
        this.capabilities = TerminalProtocols.detectCapabilities();
    }

    public TerminalNotification(PrintWriter writer, TerminalProtocols.TerminalCapabilities capabilities) {
        this.writer = writer;
        this.capabilities = capabilities;
    }

    /**
     * Show progress bar with percentage.
     *
     * @param title the title/message to show
     * @param progress percentage (0-100)
     */
    public void showProgress(String title, int progress) {
        if (!notificationActive.compareAndSet(false, true)) {
            updateProgress(title, progress);
            return;
        }

        currentTitle = title != null ? title : "";
        currentProgress = Math.max(0, Math.min(100, progress));
        startTime = Instant.now();

        String progressBar = buildProgressBar(currentTitle, currentProgress);
        writer.print(progressBar);
        writer.flush();
    }

    /**
     * Update an existing progress bar.
     */
    public void updateProgress(String title, int progress) {
        if (!notificationActive.get()) {
            showProgress(title, progress);
            return;
        }

        currentTitle = title != null ? title : "";
        currentProgress = Math.max(0, Math.min(100, progress));

        String progressBar = buildProgressBar(currentTitle, currentProgress);
        writer.print(progressBar);
        writer.flush();
    }

    /**
     * Show indeterminate progress (spinner).
     */
    public void showIndeterminate(String title) {
        if (!notificationActive.compareAndSet(false, true)) {
            updateIndeterminate(title);
            return;
        }

        currentTitle = title != null ? title : "";
        startTime = Instant.now();

        if (capabilities.isITerm2() || capabilities.isKitty()) {
            writer.print(TerminalProtocols.progressIndeterminate(currentTitle));
        } else {
            // Fallback: show spinner in terminal
            writer.print("\r" + Ansi.colored("⟳ ", AnsiColor.CYAN) + currentTitle);
        }
        writer.flush();
    }

    /**
     * Update indeterminate progress title.
     */
    public void updateIndeterminate(String title) {
        if (!notificationActive.get()) {
            showIndeterminate(title);
            return;
        }

        currentTitle = title != null ? title : "";
        updateIndeterminate();
    }

    private void updateIndeterminate() {
        if (capabilities.isITerm2() || capabilities.isKitty()) {
            writer.print(TerminalProtocols.progressIndeterminate(currentTitle));
        } else {
            // Fallback
            writer.print("\r" + Ansi.colored("⟳ ", AnsiColor.CYAN) + currentTitle);
        }
        writer.flush();
    }

    /**
     * Clear the progress bar.
     */
    public void clear() {
        if (!notificationActive.compareAndSet(true, false)) {
            return;
        }

        // Clear the line
        writer.print("\r" + TerminalProtocols.CLEAR_LINE);

        if (capabilities.isITerm2() || capabilities.isKitty()) {
            writer.print(TerminalProtocols.clearProgress());
        }

        writer.flush();
    }

    /**
     * Clear and optionally show completion message.
     */
    public void clearWithMessage(String message) {
        clear();
        if (message != null && !message.isEmpty()) {
            writer.println(message);
            writer.flush();
        }
    }

    /**
     * Show a terminal notification (bell + title).
     * Supported by iTerm2 and some other terminals.
     */
    public void notify(String title, String body) {
        if (capabilities.isITerm2()) {
            writer.print(TerminalProtocols.notification(title, body));
            writer.print(TerminalProtocols.BELL);
        } else {
            // Fallback: print notification inline
            writer.println(Ansi.colored("🔔 " + title, AnsiColor.YELLOW));
            if (body != null && !body.isEmpty()) {
                writer.println("   " + body);
            }
            writer.print(TerminalProtocols.BELL);
        }
        writer.flush();
    }

    /**
     * Show a success notification.
     */
    public void success(String title) {
        if (capabilities.isITerm2()) {
            writer.print(TerminalProtocols.notification("✓ " + title, "Success"));
            writer.print(TerminalProtocols.BELL);
        } else {
            writer.println(Ansi.colored("✓ " + title, AnsiColor.GREEN));
        }
        writer.flush();
    }

    /**
     * Show an error notification.
     */
    public void error(String title) {
        if (capabilities.isITerm2()) {
            writer.print(TerminalProtocols.notification("✗ " + title, "Error"));
            writer.print(TerminalProtocols.BELL);
        } else {
            writer.println(Ansi.colored("✗ " + title, AnsiColor.RED));
        }
        writer.flush();
    }

    /**
     * Show a warning notification.
     */
    public void warning(String title) {
        if (capabilities.isITerm2()) {
            writer.print(TerminalProtocols.notification("⚠ " + title, "Warning"));
            writer.print(TerminalProtocols.BELL);
        } else {
            writer.println(Ansi.colored("⚠ " + title, AnsiColor.YELLOW));
        }
        writer.flush();
    }

    /**
     * Show elapsed time in notification title.
     */
    public String formatElapsed() {
        if (startTime == null) return "";
        long seconds = Duration.between(startTime, Instant.now()).getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return minutes + "m " + seconds + "s";
        }
    }

    /**
     * Build a text-based progress bar for unsupported terminals.
     */
    private String buildProgressBar(String title, int progress) {
        int width = 40;
        int filled = (int) ((progress / 100.0) * width);
        int empty = width - filled;

        StringBuilder bar = new StringBuilder();
        bar.append("\r");
        bar.append(Ansi.colored("[", AnsiColor.GRAY));

        if (filled > 0) {
            bar.append(Ansi.colored("█".repeat(filled), AnsiColor.GREEN));
        }
        if (empty > 0) {
            bar.append(Ansi.colored("░".repeat(empty), AnsiColor.GRAY));
        }

        bar.append(Ansi.colored("] ", AnsiColor.GRAY));
        bar.append(String.format("%3d%% ", progress));

        if (title != null && !title.isEmpty()) {
            String elapsed = formatElapsed();
            if (!elapsed.isEmpty()) {
                bar.append(Ansi.styled(elapsed, AnsiStyle.DIM));
                bar.append(" ");
            }
            // Truncate title if needed
            String displayTitle = title;
            int maxTitle = width - 15; // Reserve space for progress bar
            if (displayTitle.length() > maxTitle) {
                displayTitle = displayTitle.substring(0, maxTitle - 3) + "...";
            }
            bar.append(displayTitle);
        }

        return bar.toString();
    }

    /**
     * Returns true if a notification is currently active.
     */
    public boolean isActive() {
        return notificationActive.get();
    }

    /**
     * Returns the current progress percentage.
     */
    public int getCurrentProgress() {
        return currentProgress;
    }

    /**
     * Returns the current notification title.
     */
    public String getCurrentTitle() {
        return currentTitle;
    }

    /**
     * Returns the capabilities of the detected terminal.
     */
    public TerminalProtocols.TerminalCapabilities getCapabilities() {
        return capabilities;
    }
}
