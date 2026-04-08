package com.claudecode.bridge;

import java.util.regex.Pattern;

/**
 * Input sanitization for webhook payloads.
 * Strips potentially dangerous content from incoming messages.
 */
public final class WebhookSanitizer {

    private WebhookSanitizer() {}

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final int MAX_PAYLOAD_SIZE = 1_048_576; // 1MB

    /**
     * Sanitizes a webhook payload string.
     * Removes script tags, HTML, and enforces size limits.
     */
    public static String sanitize(String payload) {
        if (payload == null) return "";
        if (payload.length() > MAX_PAYLOAD_SIZE) {
            payload = payload.substring(0, MAX_PAYLOAD_SIZE);
        }
        String result = SCRIPT_PATTERN.matcher(payload).replaceAll("");
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        return result.trim();
    }

    /**
     * Validates that a payload is within acceptable bounds.
     */
    public static boolean isValid(String payload) {
        return payload != null && !payload.isBlank() && payload.length() <= MAX_PAYLOAD_SIZE;
    }
}
