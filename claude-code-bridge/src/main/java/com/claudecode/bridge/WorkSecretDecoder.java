package com.claudecode.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Decodes work secrets from bridge payloads.
 * Supports Base64-encoded secrets.
 */
public final class WorkSecretDecoder {

    private WorkSecretDecoder() {}

    /**
     * Decodes a Base64-encoded work secret.
     * Returns empty if the input is null, blank, or invalid.
     */
    public static Optional<String> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded.trim());
            return Optional.of(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Encodes a work secret to Base64.
     */
    public static String encode(String secret) {
        if (secret == null) throw new IllegalArgumentException("Secret cannot be null");
        return Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
    }
}
