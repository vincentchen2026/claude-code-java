package com.claudecode.permissions;

/**
 * Source of a permission rule, indicating where the rule was defined.
 */
public enum RuleSource {
    /** Rule from user-level settings (~/.claude/settings.json). */
    USER_SETTINGS,
    /** Rule from project-level settings (.claude/settings.json). */
    PROJECT_SETTINGS,
    /** Rule from local settings (.claude/settings.local.json). */
    LOCAL_SETTINGS,
    /** Rule from feature flags. */
    FLAG_SETTINGS,
    /** Rule from organization policy. */
    POLICY_SETTINGS,
    /** Rule from CLI argument. */
    CLI_ARG,
    /** Rule from a slash command. */
    COMMAND,
    /** Rule from the current session (runtime). */
    SESSION
}
