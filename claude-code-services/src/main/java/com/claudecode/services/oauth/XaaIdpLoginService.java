package com.claudecode.services.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XaaIdpLoginService {

    private static final Logger log = LoggerFactory.getLogger(XaaIdpLoginService.class);

    private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();
    private final String idpUrl;
    private final String clientId;

    public XaaIdpLoginService(String idpUrl, String clientId) {
        this.idpUrl = idpUrl;
        this.clientId = clientId;
    }

    public LoginSession initiateLogin(String userId) {
        String sessionId = "login_" + System.currentTimeMillis();

        LoginSession session = new LoginSession(
            sessionId,
            userId,
            Instant.now(),
            LoginState.PENDING,
            null,
            null,
            null
        );

        sessions.put(sessionId, session);
        log.info("Initiated login session for user {}: {}", userId, sessionId);

        return session;
    }

    public LoginSession completeLogin(String sessionId, String authorizationCode) {
        LoginSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("Login session not found: {}", sessionId);
            return null;
        }

        LoginSession updated = new LoginSession(
            session.sessionId(),
            session.userId(),
            session.initiatedAt(),
            LoginState.AUTHENTICATED,
            authorizationCode,
            null,
            null
        );

        sessions.put(sessionId, updated);
        log.info("Completed login for session {}: {}", sessionId, session.userId());

        return updated;
    }

    public LoginSession exchangeCodeForToken(String sessionId, String code) {
        LoginSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }

        String accessToken = "token_" + System.currentTimeMillis();
        String refreshToken = "refresh_" + System.currentTimeMillis();

        LoginSession updated = new LoginSession(
            session.sessionId(),
            session.userId(),
            session.initiatedAt(),
            LoginState.TOKEN_ISSUED,
            session.authorizationCode(),
            accessToken,
            refreshToken
        );

        sessions.put(sessionId, updated);
        log.info("Exchanged code for tokens for session {}", sessionId);

        return updated;
    }

    public LoginSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public boolean isAuthenticated(String sessionId) {
        LoginSession session = sessions.get(sessionId);
        return session != null && session.state() == LoginState.TOKEN_ISSUED;
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Invalidated login session: {}", sessionId);
    }

    public record LoginSession(
        String sessionId,
        String userId,
        Instant initiatedAt,
        LoginState state,
        String authorizationCode,
        String accessToken,
        String refreshToken
    ) {}

    public enum LoginState {
        PENDING,
        AUTHENTICATED,
        TOKEN_ISSUED,
        EXPIRED,
        INVALIDATED
    }
}