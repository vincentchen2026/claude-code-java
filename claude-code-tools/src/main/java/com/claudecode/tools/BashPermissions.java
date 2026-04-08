package com.claudecode.tools;

import com.claudecode.permissions.PermissionDecision;

/**
 * Permission checks for BashTool commands.
 * Handles empty command rejection, incomplete command detection,
 * and basic safety classification.
 */
public final class BashPermissions {

    private BashPermissions() {}

    /**
     * Checks permissions for a bash command.
     *
     * @param command the command to check
     * @return the permission decision
     */
    public static PermissionDecision check(String command) {
        // Empty command — deny
        if (command == null || command.isBlank()) {
            return PermissionDecision.DENY;
        }

        // Incomplete command (trailing |, &&, ||, ;) — deny
        if (BashTool.isIncompleteCommand(command)) {
            return PermissionDecision.DENY;
        }

        // Read-only / search commands — allow
        if (BashTool.isSearchOrReadCommand(command)) {
            return PermissionDecision.ALLOW;
        }

        // Everything else — ask
        return PermissionDecision.ASK;
    }
}
