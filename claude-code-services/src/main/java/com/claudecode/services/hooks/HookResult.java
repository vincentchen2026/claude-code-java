package com.claudecode.services.hooks;

import java.util.Optional;

/**
 * Result of executing a hook command.
 */
public sealed interface HookResult
    permits HookResult.Allow, HookResult.Block, HookResult.Message, HookResult.Skip {

    /** Allow the operation to continue, optionally with additional context. */
    record Allow(Optional<String> additionalContext) implements HookResult {
        public Allow() { this(Optional.empty()); }
        public Allow(String context) { this(Optional.of(context)); }
    }

    /** Block the operation with a reason. */
    record Block(String reason, Optional<String> hookName) implements HookResult {
        public Block(String reason) { this(reason, Optional.empty()); }
    }

    /** Produce a message to inject into conversation history. */
    record Message(String content) implements HookResult {}

    /** Skip — hook didn't match or produced no output. */
    record Skip() implements HookResult {}
}
