package com.claudecode.services.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SecretScanner {

    private static final List<Pattern> SECRET_PATTERNS = List.of(
        Pattern.compile("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[\"']?([a-zA-Z0-9_-]{20,})[\"']?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(password|passwd|pwd)\\s*[:=]\\s*[\"']?([^\"'\\s]{8,})[\"']?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(secret|token|auth[_-]?token)\\s*[:=]\\s*[\"']?([a-zA-Z0-9_-]{20,})[\"']?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)bearer\\s+[a-zA-Z0-9_-]{20,}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)ghp_[a-zA-Z0-9]{36,}"),
        Pattern.compile("(?i)sk-[a-zA-Z0-9]{48,}"),
        Pattern.compile("(?i)sk-ant-[a-zA-Z0-9_-]{50,}")
    );

    private final List<Pattern> customPatterns;

    public SecretScanner() {
        this.customPatterns = new ArrayList<>();
    }

    public SecretScanner(List<String> additionalPatterns) {
        this.customPatterns = new ArrayList<>();
        for (String pattern : additionalPatterns) {
            try {
                customPatterns.add(Pattern.compile(pattern));
            } catch (Exception e) {
                // Skip invalid patterns
            }
        }
    }

    public List<SecretMatch> scan(String content) {
        List<SecretMatch> matches = new ArrayList<>();

        for (Pattern pattern : SECRET_PATTERNS) {
            scanPattern(content, pattern, matches);
        }

        for (Pattern pattern : customPatterns) {
            scanPattern(content, pattern, matches);
        }

        return matches;
    }

    public List<SecretMatch> scanFile(String filePath, String content) {
        List<SecretMatch> matches = scan(content);
        for (SecretMatch match : matches) {
            match = new SecretMatch(match.type(), match.value(), match.start(), match.end(), filePath);
        }
        return matches;
    }

    private void scanPattern(String content, Pattern pattern, List<SecretMatch> matches) {
        var matcher = pattern.matcher(content);
        while (matcher.find()) {
            String matchedValue = matcher.group();
            SecretType type = inferType(matchedValue);
            matches.add(new SecretMatch(type, maskSecret(matchedValue), matcher.start(), matcher.end(), null));
        }
    }

    private SecretType inferType(String secret) {
        String lower = secret.toLowerCase();
        if (lower.contains("github") || lower.startsWith("ghp_")) return SecretType.GITHUB_TOKEN;
        if (lower.startsWith("sk-") && !lower.contains("ant")) return SecretType.OPENAI_KEY;
        if (lower.startsWith("sk-ant-")) return SecretType.ANTHROPIC_KEY;
        if (lower.contains("api") && lower.contains("key")) return SecretType.API_KEY;
        if (lower.contains("password") || lower.contains("passwd")) return SecretType.PASSWORD;
        return SecretType.GENERIC_SECRET;
    }

    private String maskSecret(String secret) {
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    public enum SecretType {
        GITHUB_TOKEN,
        OPENAI_KEY,
        ANTHROPIC_KEY,
        API_KEY,
        PASSWORD,
        GENERIC_SECRET
    }

    public record SecretMatch(
        SecretType type,
        String value,
        int start,
        int end,
        String filePath
    ) {}
}