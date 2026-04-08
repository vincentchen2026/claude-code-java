package com.claudecode.services.hooks;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A hook matcher associates a pattern with a list of hook commands.
 * The pattern is matched against the event context (e.g., tool name).
 */
public record HookMatcher(
    Optional<String> matcher,
    List<HookCommand> hooks
) {

    /**
     * Tests whether the given query matches this matcher's pattern.
     * An empty/absent matcher matches everything.
     *
     * @param query the value to match (e.g., tool name)
     * @return true if matched
     */
    public boolean matches(String query) {
        if (matcher.isEmpty() || matcher.get().isBlank()) {
            return true;
        }
        String pattern = matcher.get();
        // Support glob-style wildcards
        if (pattern.contains("*")) {
            // Build regex by splitting on * and quoting literal parts
            StringBuilder regex = new StringBuilder("^");
            String[] parts = pattern.split("\\*", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) regex.append(".*");
                regex.append(Pattern.quote(parts[i]));
            }
            regex.append("$");
            return query != null && query.matches(regex.toString());
        }
        return pattern.equals(query);
    }
}
