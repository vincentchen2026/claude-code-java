package com.claudecode.services.tasks;

/**
 * Task type enumeration with ID prefix.
 */
public enum TaskType {
    LOCAL_BASH("b"),
    LOCAL_AGENT("a"),
    REMOTE_AGENT("r"),
    IN_PROCESS_TEAMMATE("t"),
    LOCAL_WORKFLOW("w"),
    MONITOR_MCP("m"),
    DREAM("d");

    private final String prefix;

    TaskType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
