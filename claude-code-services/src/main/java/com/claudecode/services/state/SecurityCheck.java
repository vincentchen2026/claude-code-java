package com.claudecode.services.state;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityCheck {

    private static final String HASH_ALGORITHM = "SHA-256";
    private final Map<String, CachedCheck> cache;

    public SecurityCheck() {
        this.cache = new ConcurrentHashMap<>();
    }

    public boolean checkSignature(String data, String signature, String secret) {
        String expected = computeHmac(data, secret);
        return MessageDigest.isEqual(
            signature.getBytes(),
            expected.getBytes()
        );
    }

    public boolean checkHash(String data, String expectedHash) {
        String actualHash = computeHash(data);
        return MessageDigest.isEqual(
            actualHash.getBytes(),
            expectedHash.getBytes()
        );
    }

    public boolean checkTimestamp(String timestamp, long maxAgeSeconds) {
        try {
            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - ts) <= maxAgeSeconds;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean checkCachedResult(String checkId) {
        CachedCheck cached = cache.get(checkId);
        if (cached == null) {
            return false;
        }
        if (cached.isExpired()) {
            cache.remove(checkId);
            return false;
        }
        return cached.result();
    }

    public void cacheResult(String checkId, boolean result, long ttlSeconds) {
        cache.put(checkId, new CachedCheck(result, Instant.now().plusSeconds(ttlSeconds)));
    }

    public String computeHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(data.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }

    public String computeHmac(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            var secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(data.getBytes());
            return bytesToHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void clearCache() {
        cache.clear();
    }

    private record CachedCheck(boolean result, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}