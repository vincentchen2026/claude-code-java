package com.claudecode.services.plugins;

/**
 * Represents a built-in plugin with a name@builtin ID format.
 */
public record BuiltinPlugin(
    String name,
    String description,
    boolean enabledByDefault
) {

    /**
     * Returns the plugin ID in name@builtin format.
     */
    public String id() {
        return name + "@builtin";
    }
}
