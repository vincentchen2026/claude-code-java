package com.claudecode.services.hooks;

import java.util.Map;
import java.util.Optional;

/**
 * Sealed interface for hook command types.
 * Four execution modes: bash command, LLM prompt, HTTP POST, agent verifier.
 */
public sealed interface HookCommand
    permits BashCommandHook, PromptHook, HttpHook, AgentHook {

    /** Optional if-condition using permission rule syntax (e.g., "Bash(git *)"). */
    Optional<String> ifCondition();

    /** Optional timeout in seconds. */
    Optional<Integer> timeoutSeconds();

    /** Optional status message for spinner display. */
    Optional<String> statusMessage();

    /** If true, execute once then remove. */
    boolean once();
}
