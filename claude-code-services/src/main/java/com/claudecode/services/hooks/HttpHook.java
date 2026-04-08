package com.claudecode.services.hooks;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP hook — POSTs hook input JSON to a URL.
 * Supports environment variable interpolation in headers.
 */
public record HttpHook(
    String url,
    Optional<String> ifCondition,
    Optional<Integer> timeoutSeconds,
    Map<String, String> headers,
    List<String> allowedEnvVars,
    Optional<String> statusMessage,
    boolean once
) implements HookCommand {

    public HttpHook(String url) {
        this(url, Optional.empty(), Optional.empty(), Map.of(), List.of(),
            Optional.empty(), false);
    }

    /**
     * Interpolates environment variables in header values.
     * Only variables in allowedEnvVars are substituted.
     */
    public Map<String, String> resolvedHeaders() {
        if (headers.isEmpty() || allowedEnvVars.isEmpty()) {
            return headers;
        }
        var resolved = new java.util.HashMap<>(headers);
        for (var entry : resolved.entrySet()) {
            String value = entry.getValue();
            for (String envVar : allowedEnvVars) {
                String envValue = System.getenv(envVar);
                if (envValue != null) {
                    value = value.replace("$" + envVar, envValue);
                }
            }
            entry.setValue(value);
        }
        return Map.copyOf(resolved);
    }
}
