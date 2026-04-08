package com.claudecode.services.suggestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PromptSuggestionService — returns hardcoded prompt suggestions
 * based on context keywords.
 */
public class PromptSuggestionService {

    private static final Logger LOG = LoggerFactory.getLogger(PromptSuggestionService.class);

    private static final Map<String, List<String>> KEYWORD_SUGGESTIONS = new LinkedHashMap<>();

    static {
        KEYWORD_SUGGESTIONS.put("test", List.of(
                "Write unit tests for this module",
                "Add edge case tests",
                "Generate property-based tests"
        ));
        KEYWORD_SUGGESTIONS.put("bug", List.of(
                "Help me debug this issue",
                "Find the root cause of this error",
                "Suggest a fix for this bug"
        ));
        KEYWORD_SUGGESTIONS.put("refactor", List.of(
                "Refactor this code for readability",
                "Extract common logic into a helper",
                "Simplify this function"
        ));
        KEYWORD_SUGGESTIONS.put("doc", List.of(
                "Add documentation to this module",
                "Generate API docs",
                "Write a README for this project"
        ));
        KEYWORD_SUGGESTIONS.put("perf", List.of(
                "Optimize this code for performance",
                "Profile this function",
                "Reduce memory usage"
        ));
    }

    private final Map<String, List<String>> customProviders = new LinkedHashMap<>();

    /** Get prompt suggestions based on current context. */
    public List<String> getSuggestions(String context) {
        if (context == null || context.isBlank()) {
            return List.of();
        }

        String lower = context.toLowerCase();
        List<String> results = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : KEYWORD_SUGGESTIONS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                results.addAll(entry.getValue());
            }
        }

        // Check custom providers
        for (Map.Entry<String, List<String>> entry : customProviders.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                results.addAll(entry.getValue());
            }
        }

        return results;
    }

    /** Register a custom suggestion provider. */
    public void registerProvider(String name) {
        customProviders.put(name, List.of());
        LOG.debug("Registered suggestion provider: {}", name);
    }

    /** Register a custom suggestion provider with suggestions. */
    public void registerProvider(String name, List<String> suggestions) {
        customProviders.put(name, List.copyOf(suggestions));
        LOG.debug("Registered suggestion provider: {} with {} suggestions",
                name, suggestions.size());
    }
}
