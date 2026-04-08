package com.claudecode.services.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthSystemTest {

    // --- ApiKeyAuth ---

    @Test
    void apiKeyFromConfigFile(@TempDir Path tempDir) throws IOException {
        Path keyFile = tempDir.resolve("api_key");
        Files.writeString(keyFile, "sk-test-key-12345");

        var result = ApiKeyAuth.resolve(keyFile);
        assertTrue(result.isPresent());
        assertEquals("sk-test-key-12345", result.get());
    }

    @Test
    void apiKeyFromMissingFileReturnsEmpty(@TempDir Path tempDir) {
        Path keyFile = tempDir.resolve("nonexistent");
        var result = ApiKeyAuth.resolve(keyFile);
        assertTrue(result.isEmpty());
    }

    @Test
    void apiKeyValidation() {
        assertTrue(ApiKeyAuth.isValidFormat("sk-ant-api03-abcdefghijk"));
        assertFalse(ApiKeyAuth.isValidFormat("invalid"));
        assertFalse(ApiKeyAuth.isValidFormat(null));
        assertFalse(ApiKeyAuth.isValidFormat("sk-"));
    }

    // --- SystemKeychain ---

    @Test
    void inMemoryKeychainCrud() {
        SystemKeychain keychain = SystemKeychain.inMemory();
        assertTrue(keychain.isAvailable());

        keychain.store("claude-code", "api-key", "sk-secret");
        var retrieved = keychain.retrieve("claude-code", "api-key");
        assertTrue(retrieved.isPresent());
        assertEquals("sk-secret", retrieved.get());

        assertTrue(keychain.delete("claude-code", "api-key"));
        assertTrue(keychain.retrieve("claude-code", "api-key").isEmpty());
    }

    @Test
    void inMemoryKeychainDeleteNonexistent() {
        SystemKeychain keychain = SystemKeychain.inMemory();
        assertFalse(keychain.delete("service", "account"));
    }

    // --- OAuthService ---

    @Test
    void noOpOAuthReturnsEmptyToken() {
        OAuthService oauth = OAuthService.noOp();
        assertTrue(oauth.getAccessToken().isEmpty());
    }

    @Test
    void noOpOAuthStartFlowThrows() {
        OAuthService oauth = OAuthService.noOp();
        assertThrows(UnsupportedOperationException.class, oauth::startAuthFlow);
    }

    // --- JwtTokenManager ---

    @Test
    void jwtTokenManagerSetAndGet() {
        var manager = new JwtTokenManager();
        assertTrue(manager.getValidToken().isEmpty());

        manager.setToken("test-token", 3600);
        assertTrue(manager.getValidToken().isPresent());
        assertEquals("test-token", manager.getValidToken().get());
        assertFalse(manager.isExpired());
    }

    @Test
    void jwtTokenManagerExpiration() {
        var manager = new JwtTokenManager();
        manager.setToken("test-token", 0); // expires immediately
        assertTrue(manager.isExpired());
        assertTrue(manager.getValidToken().isEmpty());
    }

    @Test
    void jwtTokenManagerClear() {
        var manager = new JwtTokenManager();
        manager.setToken("test-token", 3600);
        manager.clear();
        assertTrue(manager.getValidToken().isEmpty());
    }

    @Test
    void jwtTokenManagerExpiresWithin() {
        var manager = new JwtTokenManager();
        manager.setToken("test-token", 60);
        assertFalse(manager.expiresWithin(30));
        assertTrue(manager.expiresWithin(120));
    }

    @Test
    void extractSubjectFromJwt() {
        // Create a simple JWT-like token with a subject claim
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"sub\":\"user123\",\"exp\":9999999999}".getBytes());
        String fakeJwt = "eyJhbGciOiJIUzI1NiJ9." + payload + ".signature";

        var subject = JwtTokenManager.extractSubject(fakeJwt);
        assertTrue(subject.isPresent());
        assertEquals("user123", subject.get());
    }

    @Test
    void extractSubjectFromInvalidJwt() {
        assertTrue(JwtTokenManager.extractSubject(null).isEmpty());
        assertTrue(JwtTokenManager.extractSubject("").isEmpty());
        assertTrue(JwtTokenManager.extractSubject("not-a-jwt").isEmpty());
    }
}
