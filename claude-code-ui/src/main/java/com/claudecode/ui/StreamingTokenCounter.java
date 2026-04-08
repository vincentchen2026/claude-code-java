package com.claudecode.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-time streaming token counter for display during API responses.
 * Shows tokens/second, total tokens, and estimated time remaining.
 */
public class StreamingTokenCounter {

    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);
    private final AtomicReference<Instant> startTime = new AtomicReference<>();
    private final AtomicReference<Instant> lastUpdate = new AtomicReference<>();
    private final AtomicLong cacheReadTokens = new AtomicLong(0);
    private final AtomicLong cacheWriteTokens = new AtomicLong(0);

    private volatile long maxOutputTokens = 0;
    private volatile boolean streaming = false;

    public StreamingTokenCounter() {}

    /**
     * Start the counter (called when streaming begins).
     */
    public void start() {
        startTime.set(Instant.now());
        lastUpdate.set(Instant.now());
        streaming = true;
    }

    /**
     * Stop the counter (called when streaming ends).
     */
    public void stop() {
        streaming = false;
    }

    /**
     * Reset all counters.
     */
    public void reset() {
        inputTokens.set(0);
        outputTokens.set(0);
        cacheReadTokens.set(0);
        cacheWriteTokens.set(0);
        startTime.set(null);
        lastUpdate.set(null);
        streaming = false;
    }

    /**
     * Update token counts from API response.
     */
    public void updateTokens(long input, long output, long cacheRead, long cacheWrite) {
        inputTokens.set(input);
        outputTokens.set(output);
        cacheReadTokens.set(cacheRead);
        cacheWriteTokens.set(cacheWrite);
        lastUpdate.set(Instant.now());
    }

    /**
     * Increment output tokens (called during streaming).
     */
    public void incrementOutputTokens(long delta) {
        outputTokens.addAndGet(delta);
        lastUpdate.set(Instant.now());
    }

    /**
     * Set maximum output tokens for the request.
     */
    public void setMaxOutputTokens(long max) {
        this.maxOutputTokens = max;
    }

    /**
     * Get total input tokens.
     */
    public long getInputTokens() {
        return inputTokens.get();
    }

    /**
     * Get total output tokens.
     */
    public long getOutputTokens() {
        return outputTokens.get();
    }

    /**
     * Get cache read tokens.
     */
    public long getCacheReadTokens() {
        return cacheReadTokens.get();
    }

    /**
     * Get tokens per second rate.
     */
    public double getTokensPerSecond() {
        Instant start = startTime.get();
        if (start == null) return 0;

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        if (elapsed <= 0) return 0;

        return (outputTokens.get() * 1000.0) / elapsed;
    }

    /**
     * Get elapsed time.
     */
    public Duration getElapsed() {
        Instant start = startTime.get();
        if (start == null) return Duration.ZERO;
        return Duration.between(start, Instant.now());
    }

    /**
     * Estimate remaining time based on current rate.
     */
    public Duration estimateRemaining() {
        if (maxOutputTokens <= 0) return Duration.ZERO;

        double rate = getTokensPerSecond();
        if (rate <= 0) return Duration.ZERO;

        long remaining = maxOutputTokens - outputTokens.get();
        if (remaining <= 0) return Duration.ZERO;

        long seconds = (long) (remaining / rate);
        return Duration.ofSeconds(seconds);
    }

    /**
     * Check if streaming is active.
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Format a compact token count string.
     */
    public static String formatTokens(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format("%.1fm", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format("%.1fk", tokens / 1_000.0);
        }
        return String.valueOf(tokens);
    }

    /**
     * Format a duration string.
     */
    public static String formatDuration(Duration d) {
        long seconds = d.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return minutes + "m " + seconds + "s";
        }
    }

    /**
     * Build a compact status string for display.
     */
    public String buildStatusString() {
        StringBuilder sb = new StringBuilder();

        // Elapsed time
        Duration elapsed = getElapsed();
        if (!elapsed.isZero()) {
            sb.append(formatDuration(elapsed));
            sb.append(" ");
        }

        // Output tokens
        sb.append(Ansi.colored("↓", AnsiColor.MAGENTA));
        sb.append(formatTokens(outputTokens.get()));
        sb.append(" ");

        // Rate
        double rate = getTokensPerSecond();
        if (rate > 0) {
            sb.append(Ansi.colored("(%.1f/s)", AnsiColor.GRAY));
            sb.append(" ");
        }

        // Cache
        long cacheRead = cacheReadTokens.get();
        if (cacheRead > 0) {
            sb.append(Ansi.colored("📖", AnsiColor.CYAN));
            sb.append(formatTokens(cacheRead));
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * Build a detailed status string with progress.
     */
    public String buildDetailedStatusString() {
        StringBuilder sb = new StringBuilder();

        // Elapsed
        Duration elapsed = getElapsed();
        sb.append(Ansi.colored("⏱ ", AnsiColor.GRAY));
        sb.append(formatDuration(elapsed));
        sb.append("  ");

        // Input tokens
        sb.append(Ansi.colored("↑", AnsiColor.GREEN));
        sb.append(formatTokens(inputTokens.get()));
        sb.append("  ");

        // Output tokens with rate
        sb.append(Ansi.colored("↓", AnsiColor.MAGENTA));
        sb.append(formatTokens(outputTokens.get()));
        double rate = getTokensPerSecond();
        if (rate > 0) {
            sb.append(Ansi.styled(String.format(" (%.0f/s)", rate), AnsiStyle.DIM));
        }
        sb.append("  ");

        // Progress bar if max is set
        if (maxOutputTokens > 0) {
            int progress = (int) ((outputTokens.get() * 100) / maxOutputTokens);
            progress = Math.min(100, Math.max(0, progress));

            sb.append("[");
            int barWidth = 8;
            int filled = (progress * barWidth) / 100;
            sb.append(Ansi.colored("█".repeat(Math.max(0, filled)), AnsiColor.GREEN));
            sb.append(Ansi.colored("░".repeat(Math.max(0, barWidth - filled)), AnsiColor.GRAY));
            sb.append("]");
            sb.append(progress);
            sb.append("%");
        }

        return sb.toString();
    }

    /**
     * Build the spinner suffix with token info.
     */
    public String buildSpinnerSuffix() {
        if (!streaming) return "";

        StringBuilder sb = new StringBuilder();

        // Tokens
        sb.append(" ");
        sb.append(Ansi.colored("↓" + formatTokens(outputTokens.get()), AnsiColor.MAGENTA));

        // Rate
        double rate = getTokensPerSecond();
        if (rate > 0) {
            sb.append(Ansi.styled(String.format(" (%.0f/s)", rate), AnsiStyle.DIM));
        }

        // Cache hit indicator
        long cacheRead = cacheReadTokens.get();
        if (cacheRead > 0) {
            sb.append(Ansi.colored(" 📖", AnsiColor.CYAN));
        }

        return sb.toString();
    }
}
