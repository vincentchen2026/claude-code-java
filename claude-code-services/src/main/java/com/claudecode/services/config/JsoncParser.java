package com.claudecode.services.config;

/**
 * Parses JSONC (JSON with Comments) by stripping single-line (//) and
 * multi-line comments, plus trailing commas, producing valid JSON.
 * <p>
 * Comments inside JSON string literals are preserved.
 */
public final class JsoncParser {

    private JsoncParser() {
    }

    /**
     * Strips comments and trailing commas from JSONC, returning valid JSON.
     *
     * @param jsonc input JSONC text (may be null)
     * @return valid JSON string, or empty string if input is null/blank
     */
    public static String parse(String jsonc) {
        if (jsonc == null || jsonc.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder(jsonc.length());
        int len = jsonc.length();
        int i = 0;

        while (i < len) {
            char c = jsonc.charAt(i);

            // String literal — copy verbatim (including any "comment-like" content)
            if (c == '"') {
                int end = skipString(jsonc, i);
                result.append(jsonc, i, end);
                i = end;
                continue;
            }

            // Single-line comment: //
            if (c == '/' && i + 1 < len && jsonc.charAt(i + 1) == '/') {
                // Skip to end of line
                i += 2;
                while (i < len && jsonc.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            // Multi-line comment: /* ... */
            if (c == '/' && i + 1 < len && jsonc.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < len && !(jsonc.charAt(i) == '*' && jsonc.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 < len) {
                    i += 2; // skip */
                }
                continue;
            }

            result.append(c);
            i++;
        }

        return removeTrailingCommas(result.toString());
    }

    /**
     * Advances past a JSON string literal (handling escape sequences).
     * Returns the index just past the closing quote.
     */
    private static int skipString(String s, int start) {
        int i = start + 1; // skip opening quote
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2; // skip escaped char
            } else if (c == '"') {
                return i + 1; // past closing quote
            } else {
                i++;
            }
        }
        return i; // unterminated string — return end
    }

    /**
     * Removes trailing commas before } or ] (with optional whitespace between).
     */
    private static String removeTrailingCommas(String json) {
        // Pattern: comma followed by optional whitespace then } or ]
        return json.replaceAll(",\\s*([}\\]])", "$1");
    }
}
