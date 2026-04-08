package com.claudecode.permissions;

/**
 * Simple permission decision enum for tool permission checks.
 * Represents the three possible outcomes of a permission check.
 */
public enum PermissionDecision {
    /** Tool execution is allowed. */
    ALLOW,
    /** Tool execution is denied. */
    DENY,
    /** User should be asked for permission. */
    ASK
}
