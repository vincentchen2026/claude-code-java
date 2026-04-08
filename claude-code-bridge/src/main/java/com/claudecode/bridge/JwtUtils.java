package com.claudecode.bridge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

public final class JwtUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private JwtUtils() {}

    public static String generateSecret(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verifySignature(String token, String secret) {
        if (token == null || secret == null) {
            return false;
        }
        
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String signatureInput = parts[0] + "." + parts[1];
            String expectedSignature = hashSecret(signatureInput + secret);
            
            return MessageDigest.isEqual(
                parts[2].getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    public static Optional<String> extractSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return extractFieldFromPayload(payload, "sub");
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> extractExpiration(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String expValue = extractFieldFromPayload(payload, "exp").orElse(null);
            
            if (expValue != null) {
                return Optional.of(Long.parseLong(expValue));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean isExpired(String token) {
        Optional<Long> exp = extractExpiration(token);
        if (exp.isEmpty()) {
            return true;
        }
        return System.currentTimeMillis() / 1000 > exp.get();
    }

    private static Optional<String> extractFieldFromPayload(String payload, String field) {
        int fieldStart = payload.indexOf("\"" + field + "\"");
        if (fieldStart == -1) {
            return Optional.empty();
        }
        
        int colonPos = payload.indexOf(":", fieldStart);
        if (colonPos == -1) {
            return Optional.empty();
        }
        
        int valueStart = colonPos + 1;
        while (valueStart < payload.length() && Character.isWhitespace(payload.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= payload.length()) {
            return Optional.empty();
        }
        
        char delimiter = payload.charAt(valueStart);
        if (delimiter == '"') {
            int valueEnd = payload.indexOf('"', valueStart + 1);
            return Optional.of(payload.substring(valueStart + 1, valueEnd));
        } else {
            int valueEnd = valueStart;
            while (valueEnd < payload.length() && 
                   !Character.isWhitespace(payload.charAt(valueEnd)) && 
                   payload.charAt(valueEnd) != ',' && 
                   payload.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return Optional.of(payload.substring(valueStart, valueEnd));
        }
    }

    public static String createMinimalToken(String subject, long expirationSecs) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"sub\":\"" + subject + "\",\"exp\":" + expirationSecs + "}").getBytes(StandardCharsets.UTF_8));
        
        return header + "." + payload + ".";
    }
}