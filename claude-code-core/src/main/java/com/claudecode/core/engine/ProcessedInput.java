package com.claudecode.core.engine;

import java.util.Optional;

/**
 * Result of processing user input before sending to the API.
 * Handles slash command detection and attachment references.
 */
public record ProcessedInput(
    boolean shouldQuery,
    Optional<String> localCommandResult,
    String processedPrompt
) {

    /**
     * Creates a ProcessedInput that should be sent to the API.
     */
    public static ProcessedInput forQuery(String prompt) {
        return new ProcessedInput(true, Optional.empty(), prompt);
    }

    /**
     * Creates a ProcessedInput for a local command (no API call needed).
     */
    public static ProcessedInput forLocalCommand(String commandResult) {
        return new ProcessedInput(false, Optional.of(commandResult), "");
    }
}
