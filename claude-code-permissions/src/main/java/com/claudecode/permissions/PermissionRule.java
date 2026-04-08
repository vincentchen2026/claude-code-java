package com.claudecode.permissions;

import java.util.Optional;

/**
 * A permission rule that maps a tool name (with optional glob pattern) to a behavior.
 *
 * @param toolName  the tool name this rule applies to (e.g., "Bash", "FileWrite")
 * @param behavior  the permission behavior when this rule matches
 * @param source    where this rule was defined
 * @param pattern   optional glob pattern for finer-grained matching (e.g., "git *")
 */
public record PermissionRule(
    String toolName,
    PermissionBehavior behavior,
    RuleSource source,
    Optional<String> pattern
) {
    /**
     * Creates a rule without a pattern.
     */
    public static PermissionRule of(String toolName, PermissionBehavior behavior, RuleSource source) {
        return new PermissionRule(toolName, behavior, source, Optional.empty());
    }

    /**
     * Creates a rule with a glob pattern.
     */
    public static PermissionRule withPattern(String toolName, PermissionBehavior behavior,
                                             RuleSource source, String pattern) {
        return new PermissionRule(toolName, behavior, source, Optional.of(pattern));
    }
}
