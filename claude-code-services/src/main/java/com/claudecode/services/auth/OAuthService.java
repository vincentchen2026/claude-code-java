package com.claudecode.services.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * OAuth PKCE flow service stub.
 * Handles authorization code flow with PKCE for browser-based auth.
 */
public interface OAuthService {

    /**
     * Initiates the OAuth PKCE flow.
     * Returns the authorization URL to open in the browser.
     */
    String startAuthFlow();

    /**
     * Exchanges the authorization code for tokens.
     */
    OAuthTokens exchangeCode(String code, String codeVerifier);

    /**
     * Refreshes an expired access token.
     */
    OAuthTokens refreshToken(String refreshToken);

    /**
     * Returns the current access token if available.
     */
    Optional<String> getAccessToken();

    /**
     * OAuth token pair.
     */
    record OAuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {}

    /**
     * No-op implementation for when OAuth is not configured.
     */
    static OAuthService noOp() {
        return new NoOpOAuthService();
    }
}

class NoOpOAuthService implements OAuthService {

    private static final Logger log = LoggerFactory.getLogger(NoOpOAuthService.class);

    @Override
    public String startAuthFlow() {
        log.warn("OAuth not configured");
        throw new UnsupportedOperationException("OAuth not configured");
    }

    @Override
    public OAuthTokens exchangeCode(String code, String codeVerifier) {
        throw new UnsupportedOperationException("OAuth not configured");
    }

    @Override
    public OAuthTokens refreshToken(String refreshToken) {
        throw new UnsupportedOperationException("OAuth not configured");
    }

    @Override
    public Optional<String> getAccessToken() {
        return Optional.empty();
    }
}
