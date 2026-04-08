package com.claudecode.services.hooks;

import java.util.Optional;

/**
 * Shell command hook — executes a bash command, parses stdout as JSON.
 */
public record BashCommandHook(
    String command,
    Optional<String> ifCondition,
    Optional<String> shell,
    Optional<Integer> timeoutSeconds,
    Optional<String> statusMessage,
    boolean once,
    boolean async,
    boolean asyncRewake
) implements HookCommand {

    public BashCommandHook(String command) {
        this(command, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), false, false, false);
    }

    /** Returns the shell to use, defaulting to "bash". */
    public String effectiveShell() {
        return shell.orElse("bash");
    }
}
