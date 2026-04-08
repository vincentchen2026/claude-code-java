package com.claudecode.services.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class CryptoUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {}

    public static String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static String sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static String generateStateParam() {
        return generateSecureToken(16);
    }

    public static String generateCodeVerifier() {
        return generateSecureToken(32);
    }

    public static String generateCodeChallenge(String codeVerifier) {
        return sha256(codeVerifier);
    }

    public static boolean validateStateParam(String state, String expected) {
        if (state == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8),
                                    expected.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateSecureRandomHex(int length) {
        byte[] bytes = new byte[length / 2];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}