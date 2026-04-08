package com.claudecode.services.hooks;

/**
 * Hook event types. 27 events covering the full lifecycle:
 * tool execution, session, permissions, compact, tasks, etc.
 */
public enum HookEvent {
    PRE_TOOL_USE,
    POST_TOOL_USE,
    POST_TOOL_USE_FAILURE,
    NOTIFICATION,
    USER_PROMPT_SUBMIT,
    SESSION_START,
    SESSION_END,
    STOP,
    STOP_FAILURE,
    SUBAGENT_START,
    SUBAGENT_STOP,
    PRE_COMPACT,
    POST_COMPACT,
    PERMISSION_REQUEST,
    PERMISSION_DENIED,
    SETUP,
    TEAMMATE_IDLE,
    TASK_CREATED,
    TASK_COMPLETED,
    ELICITATION,
    ELICITATION_RESULT,
    CONFIG_CHANGE,
    WORKTREE_CREATE,
    WORKTREE_REMOVE,
    INSTRUCTIONS_LOADED,
    CWD_CHANGED,
    FILE_CHANGED;

    /**
     * Returns the lowercase dotted name for config matching (e.g., "pre_tool_use").
     */
    public String configKey() {
        return name().toLowerCase();
    }

    /**
     * Parses a config key string to a HookEvent.
     *
     * @param key the config key (case-insensitive, underscores)
     * @return the matching HookEvent
     * @throws IllegalArgumentException if no match
     */
    public static HookEvent fromConfigKey(String key) {
        return valueOf(key.toUpperCase());
    }
}
