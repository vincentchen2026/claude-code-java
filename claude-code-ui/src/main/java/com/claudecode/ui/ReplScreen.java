package com.claudecode.ui;

import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandRegistry;
import com.claudecode.commands.CommandResult;
import com.claudecode.core.engine.QueryEngine;
import com.claudecode.core.engine.SubmitOptions;
import com.claudecode.core.message.SDKMessage;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Orchestrates the full REPL experience.
 * Read input → check if slash command → dispatch to CommandRegistry or QueryEngine.
 * For QueryEngine responses, iterates SDKMessage and renders each type.
 * Shows spinner while waiting for API response.
 */
public class ReplScreen {

    private static final Logger log = LoggerFactory.getLogger(ReplScreen.class);
    private static final String DEFAULT_PROMPT = "> ";

    private final TerminalRenderer terminal;
    private final InputReader inputReader;
    private final QueryEngine queryEngine;
    private final CommandRegistry commandRegistry;
    private final CommandContext commandContext;
    private final MessageRenderDispatcher renderDispatcher;
    private final InterruptHandler interruptHandler;
    private volatile boolean running;

    public ReplScreen(
            TerminalRenderer terminal,
            InputReader inputReader,
            QueryEngine queryEngine,
            CommandRegistry commandRegistry,
            CommandContext commandContext,
            MarkdownRenderer markdownRenderer) {
        this.terminal = terminal;
        this.inputReader = inputReader;
        this.queryEngine = queryEngine;
        this.commandRegistry = commandRegistry;
        this.commandContext = commandContext;
        this.renderDispatcher = new MessageRenderDispatcher(terminal, markdownRenderer);
        this.interruptHandler = new InterruptHandler();
        this.running = false;

        // Wire up interrupt handler to QueryEngine
        interruptHandler.setApiInterruptAction(queryEngine::interrupt);
        registerSignalHandler();
    }

    /**
     * Start the REPL loop. Blocks until the user exits.
     */
    public void run() {
        running = true;
        terminal.println(Ansi.styled("Claude Code Java", AnsiStyle.BOLD));
        terminal.println(Ansi.styled("Type /help for available commands, or start chatting.", AnsiStyle.DIM));
        terminal.println("");

        while (running) {
            interruptHandler.setState(InterruptHandler.ReplState.INPUT);

            String input = inputReader.readLine(DEFAULT_PROMPT);

            // null means EOF or Ctrl+C during input
            if (input == null) {
                if (interruptHandler.isExitRequested()) {
                    break;
                }
                // Single Ctrl+C clears line, continue loop
                continue;
            }

            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (isSlashCommand(trimmed)) {
                handleSlashCommand(trimmed);
            } else {
                handleQuery(trimmed);
            }

            if (interruptHandler.isExitRequested()) {
                break;
            }
        }

        terminal.println(Ansi.styled("Goodbye!", AnsiStyle.DIM));
    }

    /**
     * Stop the REPL loop.
     */
    public void stop() {
        running = false;
    }

    /**
     * Returns true if the REPL is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check if input is a slash command.
     */
    static boolean isSlashCommand(String input) {
        return input != null && input.startsWith("/");
    }

    /**
     * Handle a slash command by dispatching to the CommandRegistry.
     */
    void handleSlashCommand(String input) {
        CommandResult result = commandRegistry.dispatch(input, commandContext);
        if (result.output() != null && !result.output().isEmpty()) {
            terminal.println(result.output());
        }
        if (result.shouldExit()) {
            running = false;
        }
    }

    /**
     * Handle a query by submitting to QueryEngine and rendering the response stream.
     */
    void handleQuery(String input) {
        Spinner spinner = new Spinner(terminal.getWriter(), "Thinking...");
        interruptHandler.setState(InterruptHandler.ReplState.API_CALL);

        try {
            spinner.start();
            Iterator<SDKMessage> messages = queryEngine.submitMessage(input, SubmitOptions.DEFAULT);
            boolean spinnerStopped = false;

            while (messages.hasNext()) {
                SDKMessage message = messages.next();

                // Stop spinner on first content message
                if (!spinnerStopped && isContentMessage(message)) {
                    spinner.stop();
                    spinnerStopped = true;
                }

                renderDispatcher.render(message);
            }

            if (!spinnerStopped) {
                spinner.stop();
            }
        } catch (Exception e) {
            spinner.stop();
            terminal.println(Ansi.colored("✗ Error: " + e.getMessage(), AnsiColor.RED));
            log.debug("Query error", e);
        } finally {
            interruptHandler.setState(InterruptHandler.ReplState.INPUT);
        }
    }

    /**
     * Returns true if this message type should stop the spinner.
     */
    static boolean isContentMessage(SDKMessage message) {
        return message instanceof SDKMessage.Assistant
                || message instanceof SDKMessage.StreamEvent
                || message instanceof SDKMessage.Error
                || message instanceof SDKMessage.Result;
    }

    /**
     * Get the interrupt handler (for testing).
     */
    InterruptHandler getInterruptHandler() {
        return interruptHandler;
    }

    /**
     * Get the render dispatcher (for testing).
     */
    MessageRenderDispatcher getRenderDispatcher() {
        return renderDispatcher;
    }

    private void registerSignalHandler() {
        try {
            Terminal jlineTerminal = terminal.getTerminal();
            jlineTerminal.handle(Terminal.Signal.INT, signal -> {
                interruptHandler.handleInterrupt();
            });
        } catch (Exception e) {
            log.debug("Could not register SIGINT handler", e);
        }
    }
}
