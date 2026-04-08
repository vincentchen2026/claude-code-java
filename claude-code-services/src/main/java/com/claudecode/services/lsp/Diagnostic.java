package com.claudecode.services.lsp;

/**
 * Represents a single LSP diagnostic (error, warning, info, hint).
 */
public record Diagnostic(
    String filePath,
    int startLine,
    int startCharacter,
    int endLine,
    int endCharacter,
    Severity severity,
    String message,
    String source,
    String code
) {

    public enum Severity {
        ERROR(1),
        WARNING(2),
        INFORMATION(3),
        HINT(4);

        private final int value;

        Severity(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static Severity fromValue(int value) {
            return switch (value) {
                case 1 -> ERROR;
                case 2 -> WARNING;
                case 3 -> INFORMATION;
                case 4 -> HINT;
                default -> INFORMATION;
            };
        }
    }

    /**
     * Format this diagnostic as a human-readable string.
     */
    public String format() {
        return String.format("%s:%d:%d: %s: %s%s",
                filePath, startLine + 1, startCharacter + 1,
                severity.name().toLowerCase(),
                message,
                source != null ? " [" + source + "]" : "");
    }
}
