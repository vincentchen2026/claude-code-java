package com.claudecode.services.hooks;

import java.util.Optional;

/**
 * LLM Prompt hook — calls an LLM to evaluate a prompt.
 * $ARGUMENTS placeholder in the prompt is replaced with hook input JSON.
 */
public record PromptHook(
    String prompt,
    Optional<String> ifCondition,
    Optional<Integer> timeoutSeconds,
    Optional<String> model,
    Optional<String> statusMessage,
    boolean once
) implements HookCommand {

    public PromptHook(String prompt) {
        this(prompt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), false);
    }
}
