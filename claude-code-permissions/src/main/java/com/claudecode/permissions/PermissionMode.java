package com.claudecode.permissions;

/**
 * Permission mode controlling the overall permission behavior.
 * Corresponds to the TS {@code PermissionMode} type.
 */
public enum PermissionMode {
    /** Default mode — ask user for permission on write operations. */
    DEFAULT,
    /** Plan mode — deny write operations, allow read operations. */
    PLAN,
    /** Bypass all permission checks — allow everything. */
    BYPASS_PERMISSIONS,
    /** Accept file edits without asking. */
    ACCEPT_EDITS,
    /** Don't ask for any permissions. */
    DONT_ASK,
    /** Auto mode — use classifier to decide. */
    AUTO
}
