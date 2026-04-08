package com.claudecode.services.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * JWT token management stub.
 * Handles token storage, validation, and refresh scheduling.
 */
public class JwtTokenManager {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenManager.class);

    private volatile String currentToken;
    private volatile Instant expiresAt;

    /**
     * Sets the current JWT token.
     */
    public void setToken(String token, long expiresInSeconds) {
        this.currentToken = token;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        log.debug("JWT token set, expires at {}", expiresAt);
    }

    /**
     * Returns the current token if it's still valid.
     */
    public Optional<String> getValidToken() {
        if (currentToken == null || isExpired()) {
            return Optional.empty();
        }
        return Optional.of(currentToken);
    }

    /**
     * Returns whether the current token is expired.
     */
    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }

    /**
     * Returns whether the token will expire within the given seconds.
     */
    public boolean expiresWithin(long seconds) {
        if (expiresAt == null) return true;
        return Instant.now().plusSeconds(seconds).isAfter(expiresAt);
    }

    /**
     * Clears the current token.
     */
    public void clear() {
        currentToken = null;
        expiresAt = null;
    }

    /**
     * Extracts the subject claim from a JWT token (stub — no signature verification).
     */
    public static Optional<String> extractSubject(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return Optional.empty();
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Simple extraction — real impl would use Jackson
            int subIdx = payload.indexOf("\"sub\"");
            if (subIdx < 0) return Optional.empty();
            int colonIdx = payload.indexOf(':', subIdx);
            int quoteStart = payload.indexOf('"', colonIdx + 1);
            int quoteEnd = payload.indexOf('"', quoteStart + 1);
            return Optional.of(payload.substring(quoteStart + 1, quoteEnd));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
