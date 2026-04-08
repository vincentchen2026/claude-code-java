package com.claudecode.ui;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Terminal rendering engine wrapping JLine3 Terminal.
 * Provides styled output, markdown rendering, and terminal size monitoring.
 */
public class TerminalRenderer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TerminalRenderer.class);

    private final Terminal terminal;
    private final boolean ownsTerminal;
    private final PrintWriter writer;
    private final List<Consumer<TerminalSize>> sizeListeners = new CopyOnWriteArrayList<>();

    /**
     * Create a renderer wrapping an existing terminal.
     */
    public TerminalRenderer(Terminal terminal) {
        this.terminal = terminal;
        this.ownsTerminal = false;
        this.writer = terminal.writer();
        registerSizeHandler();
    }

    /**
     * Create a renderer with a new JLine3 terminal (system terminal with Jansi).
     * Falls back to a dumb terminal if system terminal is unavailable.
     */
    public static TerminalRenderer create() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .jansi(true)
                    .build();
            TerminalRenderer renderer = new TerminalRenderer(terminal);
            // Mark that we own this terminal and should close it
            return new TerminalRenderer(terminal, true);
        } catch (IOException e) {
            log.warn("Failed to create system terminal, falling back to dumb terminal", e);
            return createDumb();
        }
    }

    /**
     * Create a renderer with a dumb (non-interactive) terminal.
     */
    public static TerminalRenderer createDumb() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .dumb(true)
                    .build();
            return new TerminalRenderer(terminal, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dumb terminal", e);
        }
    }

    private TerminalRenderer(Terminal terminal, boolean ownsTerminal) {
        this.terminal = terminal;
        this.ownsTerminal = ownsTerminal;
        this.writer = terminal.writer();
        registerSizeHandler();
    }

    private void registerSizeHandler() {
        terminal.handle(Terminal.Signal.WINCH, signal -> {
            TerminalSize size = getTerminalSize();
            for (Consumer<TerminalSize> listener : sizeListeners) {
                try {
                    listener.accept(size);
                } catch (Exception e) {
                    log.debug("Size listener error", e);
                }
            }
        });
    }

    /**
     * Print text without newline.
     */
    public void print(String text) {
        writer.print(text);
        writer.flush();
    }

    /**
     * Print text with newline.
     */
    public void println(String text) {
        writer.println(text);
        writer.flush();
    }

    /**
     * Print styled text with ANSI styles applied.
     */
    public void printStyled(String text, AnsiStyle... styles) {
        writer.print(Ansi.styled(text, styles));
        writer.flush();
    }

    /**
     * Print colored text.
     */
    public void printColored(String text, AnsiColor color) {
        writer.print(Ansi.colored(text, color));
        writer.flush();
    }

    /**
     * Get the current terminal size.
     */
    public TerminalSize getTerminalSize() {
        org.jline.terminal.Size size = terminal.getSize();
        if (size == null || size.getColumns() <= 0) {
            return TerminalSize.DEFAULT;
        }
        return new TerminalSize(size.getColumns(), size.getRows());
    }

    /**
     * Register a listener for terminal size changes.
     * Returns a Runnable that removes the listener when called.
     */
    public Runnable onSizeChange(Consumer<TerminalSize> listener) {
        sizeListeners.add(listener);
        return () -> sizeListeners.remove(listener);
    }

    /**
     * Get the underlying JLine terminal.
     */
    public Terminal getTerminal() {
        return terminal;
    }

    /**
     * Get the terminal writer.
     */
    public PrintWriter getWriter() {
        return writer;
    }

    @Override
    public void close() {
        if (ownsTerminal) {
            try {
                terminal.close();
            } catch (IOException e) {
                log.debug("Error closing terminal", e);
            }
        }
    }
}
