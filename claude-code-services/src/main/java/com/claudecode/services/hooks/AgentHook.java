package com.claudecode.services.hooks;

import java.util.Optional;

/**
 * Agent hook — launches an agent verifier.
 * Stub implementation; full agent integration is a placeholder.
 */
public record AgentHook(
    String prompt,
    Optional<String> ifCondition,
    Optional<Integer> timeoutSeconds,
    Optional<String> model,
    Optional<String> statusMessage,
    boolean once
) implements HookCommand {

    public AgentHook(String prompt) {
        this(prompt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), false);
    }
}
