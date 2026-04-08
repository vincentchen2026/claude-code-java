package com.claudecode.core.state;

import java.util.List;
import java.util.Map;

/**
 * Placeholder record for tool permission context.
 * Will be fully implemented in the permissions module (Task 9).
 *
 * @param mode            the current permission mode (e.g., "default", "plan", "auto")
 * @param rules           permission rules keyed by tool name
 * @param additionalDirs  additional working directories
 */
public record ToolPermissionContext(
    String mode,
    Map<String, List<String>> rules,
    List<String> additionalDirs
) {
    /** Creates an empty default context. */
    public static final ToolPermissionContext EMPTY =
        new ToolPermissionContext("default", Map.of(), List.of());
}
