package com.claudecode.commands;

import com.claudecode.core.message.Message;
import com.claudecode.core.message.Usage;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Context provided to commands during execution.
 * Provides access to engine state without tight coupling.
 */
public record CommandContext(
    /** Current model name. */
    String model,
    /** Supplier to get the current message list. */
    Supplier<List<Message>> messagesSupplier,
    /** Consumer to clear messages. */
    Runnable clearMessages,
    /** Consumer to change the model. */
    Consumer<String> setModel,
    /** Supplier for total token usage. */
    Supplier<Usage> usageSupplier,
    /** Supplier for cost calculation given usage. */
    java.util.function.ToDoubleFunction<Usage> costCalculator,
    /** Working directory path. */
    String workingDirectory,
    /** Whether running in remote/bridge mode. */
    boolean remoteMode
) {
    /**
     * Minimal context for testing.
     */
    public static CommandContext minimal() {
        return new CommandContext(
            "claude-sonnet-4-20250514",
            List::of,
            () -> {},
            model -> {},
            () -> Usage.EMPTY,
            usage -> 0.0,
            System.getProperty("user.dir"),
            false
        );
    }
}
