package com.claudecode.ui;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Animated spinner progress indicator that runs on a virtual thread.
 * Task 65 enhancements:
 * - 65.1: Dynamic verb/message spinner
 * - 65.2: Glimmer/shimmer text animation
 * - 65.3: Stalled animation detection (red spinner)
 * - 65.4: Teammate spinner tree
 * - 65.5: Spinner tips (context-aware tips)
 * - 65.6: Token budget display / thinking status / effort indicator / tool use loader
 */
public class Spinner {

    // Task 65.1: Multiple spinner frame sets for different verbs
    private static final char[][] VERB_FRAMES = {
        {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'},  // braille (default)
        {'◜', '◠', '◝', '◞', '◡', '◟'},                          // circle
        {'▁', '▃', '▄', '▅', '▆', '▇', '█', '▇', '▆', '▅', '▄', '▃'}, // bar
        {'←', '↖', '↑', '↗', '→', '↘', '↓', '↙'},                 // arrow
    };
    private static final long FRAME_INTERVAL_MS = 80;

    // Task 65.3: Stalled detection threshold (10 seconds)
    private static final long STALL_THRESHOLD_MS = 10_000;

    // Task 65.5: Spinner tips
    private static final String[] SPINNER_TIPS = {
        "Tip: Use /help to see available commands",
        "Tip: You can press Ctrl+C to interrupt",
        "Tip: Use @file to attach file context",
        "Tip: Press Tab for command completion",
        "Tip: Use /model to switch models",
        "Tip: Use /compact to reduce context",
    };

    private final PrintWriter writer;
    private final AtomicReference<String> message;
    private final AtomicReference<String> verb;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread spinnerThread;

    // Task 65.3: Stalled detection
    private volatile Instant lastActivityTime;

    // Task 65.4: Teammate spinner tree
    private final List<TeammateSpinner> teammateSpinners = new ArrayList<>();

    // Task 65.6: Token budget and thinking status
    private volatile long tokenBudget = -1;
    private volatile long currentTokenUsage = 0;
    private volatile boolean thinkingEnabled = false;
    private volatile Instant thinkingStartTime;
    private volatile int activeToolCount = 0;

    // Task 65.2: Glimmer/shimmer effect
    private volatile boolean shimmerEnabled = false;
    private volatile int shimmerOffset = 0;

    // Task 65.5: Tip rotation
    private volatile int tipIndex = 0;
    private volatile Instant lastTipTime;

    /**
     * Create a spinner that writes to the given writer.
     */
    public Spinner(PrintWriter writer) {
        this.writer = writer;
        this.message = new AtomicReference<>("");
        this.verb = new AtomicReference("");
        this.lastActivityTime = Instant.now();
        this.lastTipTime = Instant.now();
    }

    /**
     * Create a spinner that writes to the given writer with an initial message.
     */
    public Spinner(PrintWriter writer, String message) {
        this.writer = writer;
        this.message = new AtomicReference(message != null ? message : "");
        this.verb = new AtomicReference("");
        this.lastActivityTime = Instant.now();
        this.lastTipTime = Instant.now();
    }

    /**
     * Start the spinner animation on a virtual thread.
     */
    public void start() {
        start(null);
    }

    /**
     * Start the spinner animation with a status message.
     */
    public void start(String statusMessage) {
        if (statusMessage != null) {
            message.set(statusMessage);
        }
        lastActivityTime = Instant.now();
        if (running.compareAndSet(false, true)) {
            spinnerThread = Thread.ofVirtual().name("spinner").start(this::animate);
        }
    }

    /**
     * Stop the spinner and clear the line.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            Thread t = spinnerThread;
            if (t != null) {
                t.interrupt();
                try {
                    t.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Clear the spinner line
            writer.print("\r\u001B[K");
            writer.flush();
        }
    }

    /**
     * Stop the spinner and display a final message.
     */
    public void stop(String finalMessage) {
        if (running.compareAndSet(true, false)) {
            Thread t = spinnerThread;
            if (t != null) {
                t.interrupt();
                try {
                    t.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            writer.print("\r\u001B[K");
            if (finalMessage != null) {
                writer.println(finalMessage);
            }
            writer.flush();
        }
    }

    /**
     * Update the spinner status message while it's running.
     */
    public void setMessage(String newMessage) {
        message.set(newMessage != null ? newMessage : "");
        lastActivityTime = Instant.now();
    }

    /**
     * Task 65.1: Update the verb (action type) while running.
     */
    public void setVerb(String newVerb) {
        verb.set(newVerb != null ? newVerb : "");
        lastActivityTime = Instant.now();
    }

    /**
     * Task 65.3: Signal activity to reset stalled detection.
     */
    public void signalActivity() {
        lastActivityTime = Instant.now();
    }

    /**
     * Task 65.6: Set token budget for display.
     */
    public void setTokenBudget(long budget) {
        this.tokenBudget = budget;
    }

    /**
     * Task 65.6: Update current token usage.
     */
    public void setCurrentTokenUsage(long usage) {
        this.currentTokenUsage = usage;
    }

    /**
     * Task 65.6: Set thinking enabled status.
     */
    public void setThinkingEnabled(boolean enabled) {
        this.thinkingEnabled = enabled;
        if (enabled) {
            thinkingStartTime = Instant.now();
        } else {
            thinkingStartTime = null;
        }
    }

    /**
     * Task 65.6: Update active tool count.
     */
    public void setActiveToolCount(int count) {
        this.activeToolCount = count;
    }

    /**
     * Task 65.2: Enable/disable shimmer effect.
     */
    public void setShimmerEnabled(boolean enabled) {
        this.shimmerEnabled = enabled;
    }

    /**
     * Task 65.4: Add a teammate spinner to the tree.
     */
    public void addTeammate(String name, String status) {
        teammateSpinners.add(new TeammateSpinner(name, status));
    }

    /**
     * Task 65.4: Update a teammate spinner status.
     */
    public void updateTeammate(String name, String status) {
        for (TeammateSpinner ts : teammateSpinners) {
            if (ts.name.equals(name)) {
                ts.status = status;
                return;
            }
        }
        teammateSpinners.add(new TeammateSpinner(name, status));
    }

    /**
     * Task 65.4: Remove a teammate spinner.
     */
    public void removeTeammate(String name) {
        teammateSpinners.removeIf(ts -> ts.name.equals(name));
    }

    /**
     * Returns true if the spinner is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    private void animate() {
        int frame = 0;
        int tipCycle = 0;
        try {
            while (running.get()) {
                String msg = message.get();
                String currentVerb = verb.get();

                // Task 65.3: Stalled detection
                long idleMs = Duration.between(lastActivityTime, Instant.now()).toMillis();
                boolean stalled = idleMs > STALL_THRESHOLD_MS;

                // Task 65.1: Select frame set based on verb
                int frameSetIndex = selectFrameSet(currentVerb);
                char[] frames = VERB_FRAMES[frameSetIndex];
                char frameChar = frames[frame % frames.length];

                // Build the spinner line
                StringBuilder line = new StringBuilder();

                // Task 65.3: Red spinner when stalled
                if (stalled) {
                    line.append(Ansi.colored(String.valueOf(frameChar), AnsiColor.RED));
                } else {
                    line.append(Ansi.colored(String.valueOf(frameChar), AnsiColor.CYAN));
                }

                // Task 65.6: Thinking status with duration
                if (thinkingEnabled && thinkingStartTime != null) {
                    long thinkingSec = Duration.between(thinkingStartTime, Instant.now()).getSeconds();
                    line.append(Ansi.colored(" 🧠" + thinkingSec + "s", AnsiColor.MAGENTA));
                }

                // Task 65.6: Tool use loader
                if (activeToolCount > 0) {
                    line.append(Ansi.colored(" 🔧" + activeToolCount, AnsiColor.YELLOW));
                }

                line.append(" ");

                // Task 65.2: Shimmer effect
                if (shimmerEnabled && !msg.isEmpty()) {
                    line.append(applyShimmer(msg, shimmerOffset));
                    shimmerOffset = (shimmerOffset + 1) % msg.length();
                } else {
                    line.append(msg);
                }

                // Task 65.6: Token budget display
                if (tokenBudget > 0 && currentTokenUsage > 0) {
                    double pct = (double) currentTokenUsage / tokenBudget * 100;
                    line.append(Ansi.colored(String.format(" [tokens: %.0f%%]", pct), AnsiColor.GRAY));
                }

                // Task 65.5: Rotate tips every 30 seconds
                if (msg.isEmpty() && Duration.between(lastTipTime, Instant.now()).getSeconds() > 30) {
                    tipIndex = (tipIndex + 1) % SPINNER_TIPS.length;
                    line.append(Ansi.styled(SPINNER_TIPS[tipIndex], AnsiStyle.DIM));
                    lastTipTime = Instant.now();
                }

                // Task 65.4: Teammate spinner tree
                if (!teammateSpinners.isEmpty()) {
                    line.append("\n");
                    for (TeammateSpinner ts : teammateSpinners) {
                        line.append("  ").append(Ansi.colored("├─", AnsiColor.GRAY)).append(" ");
                        line.append(Ansi.colored("⠋", AnsiColor.CYAN)).append(" ");
                        line.append(ts.name).append(": ").append(Ansi.styled(ts.status, AnsiStyle.DIM));
                        line.append("\n");
                    }
                }

                // Clear line and write
                writer.print("\r\u001B[K" + line);
                writer.flush();
                frame++;
                Thread.sleep(FRAME_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Task 65.1: Select frame set based on verb.
     */
    private int selectFrameSet(String verb) {
        if (verb == null || verb.isEmpty()) return 0;
        return switch (verb.toLowerCase()) {
            case "searching", "search", "grep", "glob" -> 1; // circle
            case "loading", "reading", "writing" -> 2;       // bar
            case "moving", "navigating" -> 3;                 // arrow
            default -> 0;                                     // braille
        };
    }

    /**
     * Task 65.2: Apply shimmer effect to text.
     */
    private String applyShimmer(String text, int offset) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int pos = (i + offset) % text.length();
            if (pos < text.length() / 3) {
                sb.append(Ansi.styled(String.valueOf(text.charAt(i)), AnsiStyle.BOLD));
            } else {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Task 65.4: Teammate spinner record.
     */
    private static class TeammateSpinner {
        final String name;
        volatile String status;

        TeammateSpinner(String name, String status) {
            this.name = name;
            this.status = status;
        }
    }
}
