package com.claudecode.core.state;

import com.claudecode.core.task.TaskStateBase;

import java.util.Map;

/**
 * Application global state, using immutable record pattern.
 * Each update creates a new instance (immutable update pattern).
 * <p>
 * Corresponds to the TS {@code AppState} in {@code src/state/AppState.tsx}.
 *
 * @param toolPermissionContext the current tool permission context
 * @param tasks                active tasks keyed by task ID
 * @param fileHistory          file read/write history
 * @param attribution          attribution tracking state
 * @param fastMode             whether fast mode is enabled
 * @param extra                extensible key-value map for additional state
 */
public record AppState(
    ToolPermissionContext toolPermissionContext,
    Map<String, TaskStateBase> tasks,
    FileHistoryState fileHistory,
    AttributionState attribution,
    boolean fastMode,
    Map<String, Object> extra
) {

    /**
     * Creates the initial application state with the given permission context.
     *
     * @param permCtx the initial tool permission context
     * @return a new AppState with default values
     */
    public static AppState initial(ToolPermissionContext permCtx) {
        return new AppState(
            permCtx,
            Map.of(),
            FileHistoryState.EMPTY,
            AttributionState.EMPTY,
            false,
            Map.of()
        );
    }

    /**
     * Returns a new AppState with the tasks field replaced.
     *
     * @param tasks the new tasks map
     * @return a new AppState instance
     */
    public AppState withTasks(Map<String, TaskStateBase> tasks) {
        return new AppState(toolPermissionContext, tasks, fileHistory,
            attribution, fastMode, extra);
    }

    /**
     * Returns a new AppState with the fileHistory field replaced.
     *
     * @param fileHistory the new file history state
     * @return a new AppState instance
     */
    public AppState withFileHistory(FileHistoryState fileHistory) {
        return new AppState(toolPermissionContext, tasks, fileHistory,
            attribution, fastMode, extra);
    }

    /**
     * Returns a new AppState with the attribution field replaced.
     *
     * @param attribution the new attribution state
     * @return a new AppState instance
     */
    public AppState withAttribution(AttributionState attribution) {
        return new AppState(toolPermissionContext, tasks, fileHistory,
            attribution, fastMode, extra);
    }

    /**
     * Returns a new AppState with the fastMode field replaced.
     *
     * @param fastMode the new fast mode value
     * @return a new AppState instance
     */
    public AppState withFastMode(boolean fastMode) {
        return new AppState(toolPermissionContext, tasks, fileHistory,
            attribution, fastMode, extra);
    }

    /**
     * Returns a new AppState with the extra field replaced.
     *
     * @param extra the new extra map
     * @return a new AppState instance
     */
    public AppState withExtra(Map<String, Object> extra) {
        return new AppState(toolPermissionContext, tasks, fileHistory,
            attribution, fastMode, extra);
    }
}
